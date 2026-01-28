package com.stock.batch.global.utils;

import com.stock.batch.finance.entity.CorpFinance;
import com.stock.batch.corp.entity.CorpInfo;
import com.stock.batch.stock.entity.StockPrice;
import com.stock.batch.global.model.ApiBody;
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

public class ParseUtils {


    public static ApiBody<CorpFinance> parseCorpFinanceFromXml(String xml) throws Exception {
        ApiBody<CorpFinance> result = new ApiBody<>();

        List<CorpFinance> financeList = new ArrayList<>();

        if (StringUtils.hasText(xml)) {
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
            result.setNumOfRows(safeParseInt(getTagValue("numOfRows", doc.getDocumentElement())));
            result.setPageNo(safeParseInt(getTagValue("pageNo", doc.getDocumentElement())));
            result.setTotalCount(safeParseInt(getTagValue("totalCount", doc.getDocumentElement())));

            //item 리스트 추출 (없을 수도 있음)
            NodeList itemList = doc.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);

                financeList.add(CorpFinance.builder()
                        .basDt(DateUtils.toStringLocalDate(getTagValue("basDt", item)))
                        .bizYear(getTagValue("bizYear", item))
                        .corpCode(getTagValue("crno", item))
                        .currency(getTagValue("curCd", item))
                        .opIncome(safeParseLong(getTagValue("enpBzopPft", item)))
                        .prevOpIncome(safeParseLong(getTagValue("frmtrmBzopPft", item))) // 전기 영업이익
                        .investment(safeParseLong(getTagValue("enpCptlAmt", item)))
                        .netIncome(safeParseLong(getTagValue("enpCrtmNpf", item)))
                        .prevNetIncome(safeParseLong(getTagValue("frmtrmCrtmNpf", item))) // 전기 순이익
                        .incomeBeforeTax(safeParseLong(getTagValue("iclsPalClcAmt", item)))
                        .revenue(safeParseLong(getTagValue("enpSaleAmt", item)))
                        .prevRevenue(safeParseLong(getTagValue("frmtrmSaleAmt", item))) // 전기 매출액
                        .totalAsset(safeParseLong(getTagValue("enpTastAmt", item)))
                        .totalDebt(safeParseLong(getTagValue("enpTdbtAmt", item)))
                        .totalCapital(safeParseLong(getTagValue("enpTcptAmt", item)))
                        .docCode(getTagValue("fnclDcd", item))
                        .docName(getTagValue("fnclDcdNm", item))
                        .docDebtRatio(safeParseDouble(getTagValue("fnclDebtRto", item)))
                        .build());
            }
        }

        result.setItemList(financeList);

        return result;
    }

    private static Integer safeParseInt(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Long safeParseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static Double safeParseDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }


    public static ApiBody<StockPrice> parseStockPriceFromXml(String xml) throws Exception {
        ApiBody<StockPrice> result = new ApiBody<>();

        List<StockPrice> priceList = new ArrayList<>();

        if (StringUtils.hasText(xml)) {
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
                        .basDt(DateUtils.toStringLocalDate(getTagValue("basDt", item)))
                        .stockCode(getTagValue("srtnCd", item))
                        .marketCode(getTagValue("mrktCtg", item))
                        .volume(Integer.parseInt(getTagValue("trqu", item)))
                        .volumePrice(Long.parseLong(getTagValue("trPrc", item)))
                        .startPrice(Integer.parseInt(getTagValue("mkp", item)))
                        .endPrice(Integer.parseInt(getTagValue("clpr", item)))
                        .highPrice(Integer.parseInt(getTagValue("hipr", item)))
                        .lowPrice(Integer.parseInt(getTagValue("lopr", item)))
                        .dailyRange(Double.parseDouble(getTagValue("vs", item)))
                        .dailyRatio(Double.parseDouble(getTagValue("fltRt", item)))
                        .stockTotalCnt(Long.parseLong(getTagValue("lstgStCnt", item)))
                        .marketTotalAmt(Long.parseLong(getTagValue("mrktTotAmt", item)))
                        .build());
            }
        }

        result.setItemList(priceList);

        return result;
    }


    public static ApiBody<CorpInfo> parseCorpInfoFromXml(String xml) throws Exception {
        ApiBody<CorpInfo> result = new ApiBody<>();

        List<CorpInfo> priceList = new ArrayList<>();

        if (StringUtils.hasText(xml)) {
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

                priceList.add(CorpInfo.builder()
                        .corpName(getTagValue("itmsNm", item))
                        .stockCode(getTagValue("srtnCd", item))
                        .isinCode(getTagValue("isinCd", item))
                        .corpCode(getTagValue("crno", item))
                        .checkDt(LocalDate.now())
                        .build());
            }
        }

        result.setItemList(priceList);

        return result;
    }


    public static List<String> parseLocdatesFromXml(String xml) throws Exception {
        List<String> locdates = new ArrayList<>();

        if (StringUtils.hasText(xml)) {
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
