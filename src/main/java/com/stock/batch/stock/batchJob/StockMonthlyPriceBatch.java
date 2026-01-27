package com.stock.batch.stock.batchJob;

import com.stock.batch.stock.entity.StockMonthlyPrice;
import com.stock.batch.stock.entity.StockPrice;
import com.stock.batch.stock.repository.StockMonthlyPriceRepository;
import com.stock.batch.stock.repository.StockPriceRepository;
import com.stock.batch.stock.service.PriceCalculationService;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

@Configuration
@AllArgsConstructor
public class StockMonthlyPriceBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final DataSource dataSource;
    private final StockPriceRepository stockPriceRepository;
    private final PriceCalculationService priceCalculationService;
    private final StockMonthlyPriceRepository stockMonthlyPriceRepository;

    @Bean
    public Job monthlyStockDataJob() {
        return new JobBuilder("monthlyStockDataJob", jobRepository)
                .start(monthlyStockDataStep())
                .build();
    }

    @Bean
    public Step monthlyStockDataStep() {
        return new StepBuilder("monthlyStockDataStep", jobRepository)
                .<String, List<StockMonthlyPrice>>chunk(10, platformTransactionManager)
                .reader(stockCodeReaderMonthly()) // Use a different bean name for the reader
                .processor(monthlyStockProcessor())
                .writer(monthlyStockWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<String> stockCodeReaderMonthly() {
        return new JdbcCursorItemReaderBuilder<String>()
                .name("stockCodeReaderMonthly")
                .dataSource(dataSource)
                .sql("SELECT DISTINCT stock_code FROM TB_STOCK_PRICE")
                .rowMapper((rs, rowNum) -> rs.getString(1))
                .build();
    }

    @Bean
    public ItemProcessor<String, List<StockMonthlyPrice>> monthlyStockProcessor() {
        return stockCode -> {
            List<StockPrice> dailyPrices = stockPriceRepository.findByStockCodeOrderByBasDtAsc(stockCode);
            if (dailyPrices == null || dailyPrices.isEmpty()) {
                return null;
            }
            return priceCalculationService.calculateMonthlyPrices(dailyPrices);
        };
    }

    @Bean
    public ItemWriter<List<StockMonthlyPrice>> monthlyStockWriter() {
        return items -> {
            for (List<StockMonthlyPrice> monthlyPrices : items) {
                stockMonthlyPriceRepository.saveAll(monthlyPrices);
            }
        };
    }
}
