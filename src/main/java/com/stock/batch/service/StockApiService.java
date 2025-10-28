package com.stock.batch.service;

import com.stock.batch.consts.ApplicationConstants;
import com.stock.batch.entity.CorpFinance;
import com.stock.batch.entity.CorpInfo;
import com.stock.batch.entity.StockPrice;
import com.stock.batch.enums.StockMarket;
import com.stock.batch.utils.ParseUtils;
import com.stock.batch.utils.model.ApiBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockApiService {

    @Value("${data-go.service-key}")
    String serviceKey;

    private final WebClient webClient;

    public List<CorpInfo> getCorpInfo(String basDt) throws Exception {
        List<CorpInfo> corpList = new ArrayList<>();
        int pageNum = 1;
        int totalPage = 1;

        while (totalPage >= pageNum) {
            UriComponents uri = UriComponentsBuilder
                    .newInstance()
                    .scheme("http")
                    .host(ApplicationConstants.API_GO_URL)
                    .path(ApplicationConstants.KRX_CORP_LIST_URI)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("numOfRows", ApplicationConstants.PAGE_SIZE)
                    .queryParam("pageNo", pageNum)
                    .queryParam("basDt", basDt)
                    .build();

            String responseBody = webClient.get()
                    .uri(uri.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ApiBody<CorpInfo> result = ParseUtils.parseCorpInfoFromXml(responseBody);

            log.debug("pageNum : {} totalPage : {}" , pageNum, totalPage);
            if(pageNum == 1){
                totalPage = (int) Math.ceil((double) result.getTotalCount() / ApplicationConstants.PAGE_SIZE);
            }
            corpList.addAll(result.getItemList());

            pageNum++;
        }
            return corpList;
    }



    public List<StockPrice> getStockPrice(StockMarket marketType, String basDt) throws Exception {
        List<StockPrice> priceList = new ArrayList<>();
        int pageNum = 1;
        int totalPage = 1;

        while (totalPage >= pageNum){

            UriComponents uri = UriComponentsBuilder
                    .newInstance()
                    .scheme("http")
                    .host(ApplicationConstants.API_GO_URL)
                    .path(ApplicationConstants.KRX_STOCK_VALUE_URI)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("numOfRows", ApplicationConstants.PAGE_SIZE)
                    .queryParam("pageNo", pageNum)
                    .queryParam("mrktCls", marketType.name())
                    .queryParam("basDt", basDt)
                    .build();

            String responseBody = webClient.get()
                    .uri(uri.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ApiBody<StockPrice> result = ParseUtils.parseStockPriceFromXml(responseBody);

            log.debug("pageNum : {} totalPage : {}" , pageNum, totalPage);
            if(pageNum == 1){
                totalPage = (int) Math.ceil((double) result.getTotalCount() / ApplicationConstants.PAGE_SIZE);
            }
            priceList.addAll(result.getItemList());
            pageNum++;
        }
        return priceList;
    }

    public List<CorpFinance> getCorpFinance(String bizYear) throws Exception {
        List<CorpFinance> corpList = new ArrayList<>();
        int pageNum = 1;
        int totalPage = 1;

        while (totalPage >= pageNum) {
            UriComponents uri = UriComponentsBuilder
                    .newInstance()
                    .scheme("https")
                    .host(ApplicationConstants.API_GO_URL)
                    .path(ApplicationConstants.KRX_STOCK_FINANCE_URI)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("numOfRows", ApplicationConstants.PAGE_SIZE)
                    .queryParam("pageNo", pageNum)
                    .queryParam("bizYear", bizYear)
                    .build();

            String responseBody = webClient.get()
                    .uri(uri.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ApiBody<CorpFinance> result = ParseUtils.parseCorpFinanceFromXml(responseBody);

            log.debug("pageNum : {} totalPage : {}" , pageNum, totalPage);
            if(pageNum == 1){
                totalPage = (int) Math.ceil((double) result.getTotalCount() / ApplicationConstants.PAGE_SIZE);
            }
            corpList.addAll(result.getItemList());

            pageNum++;
        }
        return corpList;
    }






}
