package com.stock.batch.utils;

import com.stock.batch.entity.StockPrice;
import com.stock.batch.utils.model.ApiBody;
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

public class ParseUtils {

    public static ApiBody<StockPrice> parseStockPriceFromXml(String xml) throws Exception {
        ApiBody<StockPrice> result = new ApiBody<>();


        List<StockPrice> priceList = new ArrayList<>();

        if(StringUtils.hasText(xml)){
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(input);
            doc.getDocumentElement().normalize();

            //오류 응답 확인
            NodeList errorHeaders = doc.getElementsByTagName("cmmMsgHeader");
            if (errorHeaders.getLength() > 0) {
                Element error = (Element) errorHeaders.item(0);
                String errMsg = getTagValue("errMsg", error);
                String returnAuthMsg = getTagValue("returnAuthMsg", error);
                throw new RuntimeException("API 오류: " + errMsg + " - " + returnAuthMsg);
            }

            //API 호출 값 확인
            result.setNumOfRows(Integer.parseInt(getTagValue("numOfRows", doc.getDocumentElement())));
            result.setPageNo(Integer.parseInt(getTagValue("pageNo", doc.getDocumentElement())));
            result.setTotalCount(Integer.parseInt(getTagValue("totalCount", doc.getDocumentElement())));

            //item 리스트 추출 (없을 수도 있음)
            NodeList itemList = doc.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);

                priceList.add(StockPrice.builder()
                        .basDt(DateUtils.toStringLocalDate(getTagValue("basDt",item )))
                        .stockCode(getTagValue("srtnCd",item))
                        .marketCode(getTagValue("mrktCtg",item))
                        .volume(Integer.parseInt(getTagValue("trqu",item)))
                        .volumePrice(Long.parseLong(getTagValue("trPrc",item)))
                        .startPrice(Integer.parseInt(getTagValue("mkp",item)))
                        .endPrice(Integer.parseInt(getTagValue("clpr",item)))
                        .highPrice(Integer.parseInt(getTagValue("hipr",item)))
                        .lowPrice(Integer.parseInt(getTagValue("lopr",item)))
                        .dailyRange(Double.parseDouble(getTagValue("vs",item)))
                        .dailyRatio(Double.parseDouble(getTagValue("fltRt",item)))
                        .stockTotalCnt(Long.parseLong(getTagValue("lstgStCnt",item)))
                        .marketTotalAmt(Long.parseLong(getTagValue("mrktTotAmt",item)))
                        .build() );
            }
        }

        result.setItemList(priceList);

        return result;
    }


    public static List<String> parseLocdatesFromXml(String xml) throws Exception {
        List<String> locdates = new ArrayList<>();

        if(StringUtils.hasText(xml)){
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(input);
            doc.getDocumentElement().normalize();

            //오류 응답 확인
            NodeList errorHeaders = doc.getElementsByTagName("cmmMsgHeader");
            if (errorHeaders.getLength() > 0) {
                Element error = (Element) errorHeaders.item(0);
                String errMsg = getTagValue("errMsg", error);
                String returnAuthMsg = getTagValue("returnAuthMsg", error);
                throw new RuntimeException("API 오류: " + errMsg + " - " + returnAuthMsg);
            }

            //item 리스트 추출 (없을 수도 있음)
            NodeList itemList = doc.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);
                String locdate = getTagValue("locdate", item);
                if (!locdate.isEmpty()) {
                    locdates.add(locdate);
                }
            }
        }
        return locdates;
    }

    private static String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0 && nodeList.item(0).getFirstChild() != null) {
            return nodeList.item(0).getFirstChild().getNodeValue();
        }
        return "";
    }

}
