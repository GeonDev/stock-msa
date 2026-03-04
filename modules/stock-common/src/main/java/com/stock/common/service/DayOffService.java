package com.stock.common.service;

import com.stock.common.consts.ApplicationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static com.stock.common.utils.DateUtils.toLocalDateString;
import static com.stock.common.utils.ParseUtils.parseLocdatesFromXml;

@Slf4j
@Service
@RequiredArgsConstructor
public class DayOffService {

    @Value("${data-go.service-key:}")
    String serviceKey;

    private final RestClient restClient;

    // 대한민국 공휴일 체크 - 불필요한 배치 수행 안함 (true: 휴일/주말, false: 평일)
    @Cacheable(value = "holidayCache", key = "#targetDate.toString()")
    public boolean checkIsDayOff(LocalDate targetDate) {

        // 6,7 (토요일, 일요일) -> 휴일이므로 true 반환
         if(targetDate.getDayOfWeek().getValue() > 5 ){
             return true;
         }

         if (!StringUtils.hasText(serviceKey) || serviceKey.contains("YOUR_SERVICE_KEY")) {
             // 서비스 키가 없으면 주말만 체크
             return false;
         }

        //URI 생성
        String solMonth =  String.format("%02d", targetDate.getMonthValue());
        
        try {
            String decodedServiceKey = java.net.URLDecoder.decode(serviceKey, java.nio.charset.StandardCharsets.UTF_8);

            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("http")
                    .host(ApplicationConstants.API_GO_URL)
                    .path(ApplicationConstants.KAI_REST_DATE_URL)
                    .queryParam("solYear", targetDate.getYear())
                    .queryParam("solMonth", solMonth)
                    .queryParam("ServiceKey", decodedServiceKey)
                    .queryParam("numOfRows", 10)
                    .build()
                    .toUri();

            //API 호출 (RestClient)
            String responseXml = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            List<String> dateList = parseLocdatesFromXml(responseXml);
            // 휴일 목록에 포함되어 있으면 true
            return dateList.contains(toLocalDateString(targetDate));
        } catch (Exception e){
            log.warn("Failed to fetch holidays from external API: {}", e.getMessage());
            // 오류 발생 시 평일로 간주 (false)하여 배치 수행 시도
            return false;
        }
    }
}
