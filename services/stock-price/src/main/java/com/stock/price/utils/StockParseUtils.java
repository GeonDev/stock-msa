package com.stock.price.utils;

import com.stock.price.entity.StockPrice;
import com.stock.common.model.ApiBody;
import com.stock.common.utils.DateUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.stock.common.utils.ParseUtils.*;

public class StockParseUtils {

    public static ApiBody<StockPrice> parseStockPriceFromXml(String xml) throws Exception {
        ApiBody<StockPrice> result = new ApiBody<>();
        List<StockPrice> priceList = new ArrayList<>();

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

                priceList.add(StockPrice.builder()
                        .basDt(DateUtils.toStringLocalDate(getTagValue("basDt", item)))
                        .stockCode(getTagValue("srtnCd", item))
                        .marketCode(getTagValue("mrktCtg", item))
                        .volume(safeParseBigDecimal(getTagValue("trqu", item)))
                        .volumePrice(safeParseBigDecimal(getTagValue("trPrc", item)))
                        .startPrice(safeParseBigDecimal(getTagValue("mkp", item)))
                        .endPrice(safeParseBigDecimal(getTagValue("clpr", item)))
                        .highPrice(safeParseBigDecimal(getTagValue("hipr", item)))
                        .lowPrice(safeParseBigDecimal(getTagValue("lopr", item)))
                        .dailyRange(safeParseBigDecimal(getTagValue("vs", item)))
                        .dailyRatio(safeParseBigDecimal(getTagValue("fltRt", item)))
                        .stockTotalCnt(safeParseBigDecimal(getTagValue("lstgStCnt", item)))
                        .marketTotalAmt(safeParseBigDecimal(getTagValue("mrktTotAmt", item)))
                        .build());
            }
        }
        result.setItemList(priceList);
        return result;
    }
}
