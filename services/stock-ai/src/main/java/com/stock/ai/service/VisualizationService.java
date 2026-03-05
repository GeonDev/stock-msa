package com.stock.ai.service;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class VisualizationService {

    public byte[] createIndicatorChart(String corpName, CorpFinanceIndicatorDto indicators) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        if (indicators.getPer() != null) dataset.addValue(indicators.getPer().doubleValue(), "PER", "Valuation");
        if (indicators.getPbr() != null) dataset.addValue(indicators.getPbr().doubleValue(), "PBR", "Valuation");
        if (indicators.getRoe() != null) dataset.addValue(indicators.getRoe().doubleValue(), "ROE (%)", "Profitability");
        if (indicators.getOperatingMargin() != null) dataset.addValue(indicators.getOperatingMargin().doubleValue(), "Op Margin (%)", "Profitability");

        JFreeChart chart = ChartFactory.createBarChart(
                corpName + " Financial Indicators",
                "Category",
                "Value",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(baos, chart, 600, 400);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate chart", e);
        }
    }
}
