package com.stock.ai.service;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.common.dto.CorpInfoDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PromptService {

    public String createFinancialSummary(CorpInfoDto corpInfo, CorpFinanceIndicatorDto indicators) {
        StringBuilder sb = new StringBuilder();
        sb.append("Company Name: ").append(corpInfo.getCorpName()).append("\n");
        sb.append("Market: ").append(corpInfo.getMarket()).append("\n");
        sb.append("Sector: ").append(corpInfo.getSector()).append("\n");
        sb.append("Reference Date: ").append(indicators.getBasDt()).append("\n\n");

        sb.append("[Profitability]\n");
        sb.append("- ROE: ").append(formatBigDecimal(indicators.getRoe())).append("%\n");
        sb.append("- ROA: ").append(formatBigDecimal(indicators.getRoa())).append("%\n");
        sb.append("- Operating Margin: ").append(formatBigDecimal(indicators.getOperatingMargin())).append("%\n");
        sb.append("- Net Margin: ").append(formatBigDecimal(indicators.getNetMargin())).append("%\n\n");

        sb.append("[Valuation]\n");
        sb.append("- PER: ").append(formatBigDecimal(indicators.getPer())).append("\n");
        sb.append("- PBR: ").append(formatBigDecimal(indicators.getPbr())).append("\n");
        sb.append("- PSR: ").append(formatBigDecimal(indicators.getPsr())).append("\n");
        sb.append("- EV/EBITDA: ").append(formatBigDecimal(indicators.getEvEbitda())).append("\n\n");

        sb.append("[Growth (YoY)]\n");
        sb.append("- Revenue Growth: ").append(formatBigDecimal(indicators.getYoyRevenueGrowth())).append("%\n");
        sb.append("- Operating Income Growth: ").append(formatBigDecimal(indicators.getYoyOpIncomeGrowth())).append("%\n");
        sb.append("- Net Income Growth: ").append(formatBigDecimal(indicators.getYoyNetIncomeGrowth())).append("%\n");

        return sb.toString();
    }

    public String createScoringPrompt(String financialSummary) {
        return "Analyze the following financial data and provide a score from 0 to 100 for 'Profitability', 'Stability', and 'Growth'.\n" +
               "Also provide a short 'AI Opinion' (within 3 sentences) in Korean.\n\n" +
               financialSummary + "\n\n" +
               "Response format:\n" +
               "Profitability: [score]\n" +
               "Stability: [score]\n" +
               "Growth: [score]\n" +
               "Opinion: [Korean opinion]";
    }

    private String formatBigDecimal(BigDecimal value) {
        return Optional.ofNullable(value)
                .map(v -> v.setScale(2, java.math.RoundingMode.HALF_UP).toString())
                .orElse("N/A");
    }
}
