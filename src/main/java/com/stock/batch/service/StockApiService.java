package com.stock.batch.service;

import com.stock.batch.consts.ApplicationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.tinylog.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static com.stock.batch.utils.CommonUtils.parseLocdatesFromXml;
import static com.stock.batch.utils.DateUtils.toLocalDateString;

@Service
@RequiredArgsConstructor
public class StockApiService {

    @Value("${data-go.service-key}")
    String serviceKey;

    private final WebClient webClient;







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
