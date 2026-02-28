package com.stock.price.service;


import com.stock.common.consts.ApplicationConstants;
import com.stock.common.dto.StockPriceDto;
import com.stock.common.enums.StockMarket;
import com.stock.common.model.ApiBody;
import com.stock.common.utils.DateUtils;
import com.stock.price.entity.StockPrice;
import com.stock.price.mapper.StockPriceMapper;
import com.stock.price.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.stock.price.utils.StockParseUtils.parseStockPriceFromXml;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    @Value("${data-go.service-key}")
    String serviceKey;

    private final RestClient restClient;
    private final StockPriceRepository stockPriceRepository;
    private final StockPriceMapper stockPriceMapper;
    private final com.stock.price.repository.StockIndicatorRepository stockIndicatorRepository;
    private final com.stock.price.mapper.StockIndicatorMapper stockIndicatorMapper;


    public List<StockPrice> getStockPrice(StockMarket marketType, String basDt) throws Exception {
        List<StockPrice> priceList = new ArrayList<>();
        int pageNum = 1;
        int totalPage = 1;

        String decodedServiceKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);

        while (totalPage >= pageNum) {

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
                log.debug("pageNum : {} totalPage : {}", pageNum, totalPage);
                if (pageNum == 1) {
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


    @Cacheable(value = "latestPriceCache", key = "#stockCode")
    public StockPriceDto getLatestStockPrice(String stockCode) {
        return stockPriceRepository.findFirstByStockCodeOrderByBasDtDesc(stockCode.replace("A", ""))
                .map(stockPriceMapper::toDto)
                .orElse(null);
    }

    @Cacheable(value = "priceCache", key = "#stockCode + ':' + #date")
    public StockPriceDto getPriceByDate(String stockCode, String date) {

        LocalDate localDate = DateUtils.toStringLocalDate(date);

        return stockPriceRepository.findByStockCodeAndBasDt(stockCode.replace("A", ""), localDate)
                .map(stockPriceMapper::toDto)
                .orElse(null);
    }

    public List<StockPriceDto> getPricesByDateBatch(List<String> stockCodes, String date) {

        LocalDate localDate = DateUtils.toStringLocalDate(date);
        List<String> codes = stockCodes.stream().map(c -> c.replace("A", "")).toList();

        return stockPriceRepository.findByStockCodeInAndBasDt(codes, localDate).stream()
                .map(stockPriceMapper::toDto)
                .toList();
    }

    public List<com.stock.common.dto.StockIndicatorDto> getIndicatorsByDateBatch(List<String> stockCodes, String date) {
        LocalDate localDate = DateUtils.toStringLocalDate(date);
        List<String> codes = stockCodes.stream().map(c -> c.replace("A", "")).toList();
        return stockIndicatorRepository.findByStockCodesAndBasDt(codes, localDate).stream()
                .map(stockIndicatorMapper::toDto)
                .toList();
    }

    public List<StockPriceDto> getPriceHistory(String stockCode, int days) {
        String code = stockCode.replace("A", "");
        
        // 데이터가 최신이 아닐 수 있으므로, 해당 종목의 가장 최근 날짜를 기준으로 가져옴
        LocalDate lastDate = stockPriceRepository.findFirstByStockCodeOrderByBasDtDesc(code)
                .map(StockPrice::getBasDt)
                .orElse(LocalDate.now());
        
        LocalDate startDate = lastDate.minusDays(days);
        LocalDate endDate = lastDate.plusDays(1);
        
        log.debug("Fetching price history for {} from {} to {}", code, startDate, endDate);
        
        return stockPriceRepository.findByStockCodeAndBasDtBetweenOrderByBasDtAsc(
                code, startDate, endDate).stream()
                .map(stockPriceMapper::toDto)
                .toList();
    }

    public List<StockPriceDto> getPriceHistory(String stockCode, String startDate, String endDate) {
        String code = stockCode.replace("A", "");
        LocalDate start = DateUtils.toStringLocalDate(startDate);
        LocalDate end = DateUtils.toStringLocalDate(endDate);

        return stockPriceRepository.findByStockCodeAndBasDtBetweenOrderByBasDtAsc(
                code, start, end).stream()
                .map(stockPriceMapper::toDto)
                .toList();
    }
}
