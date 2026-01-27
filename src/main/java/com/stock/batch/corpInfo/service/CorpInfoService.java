package com.stock.batch.corpInfo.service;


import com.stock.batch.corpInfo.entity.CorpInfo;
import com.stock.batch.global.consts.ApplicationConstants;
import com.stock.batch.global.model.ApiBody;
import com.stock.batch.global.utils.ParseUtils;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CorpInfoService {

    @Value("${data-go.service-key}")
    String serviceKey;

    private final RestClient restClient;

    public List<CorpInfo> getCorpInfo(String basDt) throws Exception {
        List<CorpInfo> corpList = new ArrayList<>();
        int pageNum = 1;
        int totalPage = 1;

        String decodedServiceKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);

        while (totalPage >= pageNum) {
            UriComponents uri = UriComponentsBuilder
                    .newInstance()
                    .scheme("http")
                    .host(ApplicationConstants.API_GO_URL)
                    .path(ApplicationConstants.KRX_CORP_LIST_URI)
                    .queryParam("serviceKey", decodedServiceKey)
                    .queryParam("numOfRows", ApplicationConstants.PAGE_SIZE)
                    .queryParam("pageNo", pageNum)
                    .queryParam("basDt", basDt)
                    .build();

            String responseBody = restClient.get()
                    .uri(uri.toUri())
                    .retrieve()
                    .body(String.class);

            try {
                ApiBody<CorpInfo> result = ParseUtils.parseCorpInfoFromXml(responseBody);
                log.debug("pageNum : {} totalPage : {}" , pageNum, totalPage);
                if(pageNum == 1){
                    totalPage = (int) Math.ceil((double) result.getTotalCount() / ApplicationConstants.PAGE_SIZE);
                }
                corpList.addAll(result.getItemList());
                pageNum++;
            } catch (Exception e) {
                log.error("Failed to parse XML response: {}", responseBody);
                throw e;
            }
        }
            return corpList;
    }
}