package com.stock.corp.service;


import com.stock.common.dto.CorpInfoDto;
import com.stock.corp.entity.CorpInfo;
import com.stock.corp.mapper.CorpInfoMapper;
import com.stock.common.consts.ApplicationConstants;
import com.stock.common.model.ApiBody;
import com.stock.corp.repository.CorpInfoRepository;
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

import static com.stock.corp.utils.CorpParseUtils.parseCorpInfoFromXml;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorpInfoService {

    @Value("${data-go.service-key}")
    String serviceKey;

    private final RestClient restClient;
    private final CorpInfoRepository corpInfoRepository;
    private final CorpInfoMapper corpInfoMapper;

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
                ApiBody<CorpInfo> result = parseCorpInfoFromXml(responseBody);
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


    public CorpInfoDto getCorpInfoByCorpCode(String corpCode) {
        return corpInfoRepository.findById(corpCode)
                .map(corpInfoMapper::toDto)
                .orElse(null);
    }

    public List<CorpInfoDto> getAllValidCorpInfos() {
        return corpInfoMapper.toDtoList(corpInfoRepository.findAllByStockCodeIsNotNull());
    }

    public List<CorpInfoDto> getCorpsByMarket(String market) {
        return corpInfoMapper.toDtoList(corpInfoRepository.findAllByMarket(market));
    }

    public CorpInfo getCorpInfoByStockCode(String stockCode) {
        return corpInfoRepository.findByStockCode(stockCode).orElse(null);
    }
}