package com.stock.stock.service;


import com.stock.common.consts.ApplicationConstants;
import com.stock.common.enums.StockMarket;
import com.stock.common.model.ApiBody;
import com.stock.stock.entity.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.stock.stock.utils.StockParseUtils.parseStockPriceFromXml;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    @Value("${data-go.service-key}")
    String serviceKey;

    private final RestClient restClient;


    public List<StockPrice> getStockPrice(StockMarket marketType, String basDt) throws Exception {
        List<StockPrice> priceList = new ArrayList<>();
        int pageNum = 1;
        int totalPage = 1;

        String decodedServiceKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);

        while (totalPage >= pageNum){

            UriComponents uri = UriComponentsBuilder
                    .newInstance()
                    .scheme("http")
                    .host(ApplicationConstants.API_GO_URL)
                    .path(ApplicationConstants.KRX_STOCK_VALUE_URI)
                    .queryParam("serviceKey", decodedServiceKey)
                    .queryParam("numOfRows", ApplicationConstants.PAGE_SIZE)
                    .queryParam("pageNo", pageNum)
                    .queryParam("mrktCls", marketType.name())
                    .queryParam("basDt", basDt)
                    .build();

            log.debug("Request URI: {}", uri.toUri());

            String responseBody = restClient.get()
                    .uri(uri.toUri())
                    .retrieve()
                    .body(String.class);

            try {
                ApiBody<StockPrice> result = parseStockPriceFromXml(responseBody);
                log.debug("pageNum : {} totalPage : {}" , pageNum, totalPage);
                if(pageNum == 1){
                    totalPage = (int) Math.ceil((double) result.getTotalCount() / ApplicationConstants.PAGE_SIZE);
                }
                priceList.addAll(result.getItemList());
                pageNum++;
            } catch (Exception e) {
                log.error("Failed to parse XML response for date {}: {}", basDt, responseBody);
                throw e;
            }
        }
        return priceList;
    }

    private final com.stock.stock.repository.StockPriceRepository stockPriceRepository;

    public StockPrice getLatestStockPrice(String stockCode) {
        return stockPriceRepository.findFirstByStockCodeOrderByBasDtDesc(stockCode).orElse(null);
    }


}