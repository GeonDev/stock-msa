package com.stock.batch.service;

import com.stock.batch.consts.ApplicationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static com.stock.batch.utils.DateUtils.toLocalDateString;
import static com.stock.batch.utils.ParseUtils.parseLocdatesFromXml;

@Slf4j
@Service
@RequiredArgsConstructor
public class DayOffService {

    @Value("${data-go.service-key}")
    String serviceKey;

    private final WebClient webClient;

    // 대한민국 공휴일 체크 - 불필요한 배치 수행 안함
    public boolean checkIsDayOff(LocalDate targetDate) {

        // 6,7 (토요일, 일요일)
         if(targetDate.getDayOfWeek().getValue() > 5 ){
             return false;
         }


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
