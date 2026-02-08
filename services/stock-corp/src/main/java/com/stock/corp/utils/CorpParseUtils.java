package com.stock.corp.utils;

import com.stock.corp.entity.CorpInfo;
import com.stock.common.model.ApiBody;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.stock.common.utils.ParseUtils.*;

public class CorpParseUtils {

    public static ApiBody<CorpInfo> parseCorpInfoFromXml(String xml) throws Exception {
        ApiBody<CorpInfo> result = new ApiBody<>();
        List<CorpInfo> priceList = new ArrayList<>();

        if (StringUtils.hasText(xml)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(input);
            doc.getDocumentElement().normalize();

            NodeList errorHeaders = doc.getElementsByTagName("cmmMsgHeader");
            if (errorHeaders.getLength() > 0) {
                Element error = (Element) errorHeaders.item(0);
                String errMsg = getTagValue("errMsg", error);
                String returnAuthMsg = getTagValue("returnAuthMsg", error);
                throw new RuntimeException("API 오류: " + errMsg + " - " + returnAuthMsg);
            }

            result.setNumOfRows(safeParseInt(getTagValue("numOfRows", doc.getDocumentElement())));
            result.setPageNo(safeParseInt(getTagValue("pageNo", doc.getDocumentElement())));
            result.setTotalCount(safeParseInt(getTagValue("totalCount", doc.getDocumentElement())));

            NodeList itemList = doc.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);

                priceList.add(CorpInfo.builder()
                        .corpName(getTagValue("itmsNm", item))
                        .stockCode(getTagValue("srtnCd", item))
                        .isinCode(getTagValue("isinCd", item))
                        .corpCode(getTagValue("crno", item))
                        .market(getTagValue("mrktCtg", item))
                        .checkDt(LocalDate.now())
                        .build());
            }
        }
        result.setItemList(priceList);
        return result;
    }
}
