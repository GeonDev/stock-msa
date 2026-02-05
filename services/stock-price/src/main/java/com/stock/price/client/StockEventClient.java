package com.stock.price.client;

import com.stock.common.consts.ApplicationConstants;
import com.stock.common.dto.StockIssuanceInfoDto;
import com.stock.common.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventClient {

    @Value("${data-go.service-key}")
    private String serviceKey;

    private final RestClient restClient;

    public List<StockIssuanceInfoDto> getIssuanceInfo(String stockCode) {
        List<StockIssuanceInfoDto> result = new ArrayList<>();
        try {
            String decodedKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);

            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("http")
                    .host(ApplicationConstants.API_GO_URL)
                    .path(ApplicationConstants.KRX_STOCK_ISSUANCE_URI)
                    .queryParam("serviceKey", decodedKey)
                    .queryParam("numOfRows", 1000)
                    .queryParam("pageNo", 1)
                    .queryParam("resultType", "xml")
                    .queryParam("srtIsinCd", stockCode)
                    .build()
                    .toUri();

            String responseXml = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                    .retrieve()
                    .body(String.class);

            result = parseIssuanceInfo(responseXml);

        } catch (Exception e) {
            log.error("Failed to fetch issuance info for {}: {}", stockCode, e.getMessage());
        }
        return result;
    }

    private List<StockIssuanceInfoDto> parseIssuanceInfo(String xml) throws Exception {
        List<StockIssuanceInfoDto> list = new ArrayList<>();
        if (xml == null || xml.isEmpty()) return list;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();

        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            
            StockIssuanceInfoDto dto = StockIssuanceInfoDto.builder()
                    .stockCode(ParseUtils.getTagValue("srtIsinCd", item))
                    .eventType(ParseUtils.getTagValue("scrsItmsKndNm", item))
                    .eventDate(ParseUtils.getTagValue("listDd", item))
                    // Note: API might not provide direct 'ratio'. 
                    // Need to check actual response or calculate later.
                    // For now, mapping available amount/qty fields if tags match standard KRX.
                    .issuanceAmount(ParseUtils.safeParseBigDecimal(ParseUtils.getTagValue("isuAmt", item)))
                    .listedStockCnt(ParseUtils.safeParseBigDecimal(ParseUtils.getTagValue("lstStkCnt", item)))
                    .build();
            
            list.add(dto);
        }
        return list;
    }
}
