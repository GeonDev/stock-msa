package com.stock.batch.service;

import com.stock.batch.consts.ApplicationConstants;
import com.stock.batch.entity.StockPrice;
import com.stock.batch.enums.StockType;
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

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.stock.batch.utils.ParseUtils.*;
import static com.stock.batch.utils.DateUtils.toLocalDateString;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockApiService {

    @Value("${data-go.service-key}")
    String serviceKey;

    private final WebClient webClient;


    public List<StockPrice> getStockPrice(StockType marketType, String basDt) throws Exception {
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




    // 대한민국 공휴일 체크 - 불필요한 배치 수행 안함
    public boolean checkIsDayOff(LocalDate targetDate) {
        //URI 생성
        String solMonth =  String.format("%02d", targetDate.getMonthValue());

        URI uri = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(ApplicationConstants.API_GO_URL)
                .path(ApplicationConstants.KAI_REST_DATE_URL)
                .queryParam("solYear", targetDate.getYear())
                .queryParam("solMonth", solMonth)
                .queryParam("ServiceKey", serviceKey)
                .queryParam("numOfRows", 10)
                .build()
                .toUri();

        //API 호출 (WebClient)
        String responseXml = webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();  // 동기 호출

        try {
            List<String> dateList = parseLocdatesFromXml(responseXml);
            return dateList.contains(toLocalDateString(targetDate));
        }catch (Exception e){
            return false;
        }
    }

}
