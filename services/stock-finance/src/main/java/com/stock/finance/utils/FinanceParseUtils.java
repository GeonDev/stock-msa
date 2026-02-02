package com.stock.finance.utils;

import com.stock.finance.entity.CorpFinance;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.stock.common.utils.ParseUtils.*;

public class FinanceParseUtils {

    public static ApiBody<CorpFinance> parseCorpFinanceFromXml(String xml) throws Exception {
        ApiBody<CorpFinance> result = new ApiBody<>();
        List<CorpFinance> financeList = new ArrayList<>();

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

                financeList.add(CorpFinance.builder()
                        .basDt(DateUtils.toStringLocalDate(getTagValue("basDt", item)))
                        .bizYear(getTagValue("bizYear", item))
                        .corpCode(getTagValue("crno", item))
                        .currency(getTagValue("curCd", item))
                        .opIncome(safeParseLong(getTagValue("enpBzopPft", item)))
                        .prevOpIncome(safeParseLong(getTagValue("frmtrmBzopPft", item)))
                        .investment(safeParseLong(getTagValue("enpCptlAmt", item)))
                        .netIncome(safeParseLong(getTagValue("enpCrtmNpf", item)))
                        .prevNetIncome(safeParseLong(getTagValue("frmtrmCrtmNpf", item)))
                        .incomeBeforeTax(safeParseLong(getTagValue("iclsPalClcAmt", item)))
                        .revenue(safeParseLong(getTagValue("enpSaleAmt", item)))
                        .prevRevenue(safeParseLong(getTagValue("frmtrmSaleAmt", item)))
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
}
