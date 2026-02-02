package com.stock.stock.batchJob;

import com.stock.stock.entity.StockPrice;
import com.stock.stock.entity.StockWeeklyPrice;
import com.stock.stock.repository.StockPriceRepository;
import com.stock.stock.repository.StockWeeklyPriceRepository;
import com.stock.stock.service.PriceCalculationService;
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
public class StockWeeklyPriceBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final DataSource dataSource;
    private final StockPriceRepository stockPriceRepository;
    private final PriceCalculationService priceCalculationService;
    private final StockWeeklyPriceRepository stockWeeklyPriceRepository;

    @Bean
    public Job weeklyStockDataJob() {
        return new JobBuilder("weeklyStockDataJob", jobRepository)
                .start(weeklyStockDataStep())
                .build();
    }

    @Bean
    public Step weeklyStockDataStep() {
        return new StepBuilder("weeklyStockDataStep", jobRepository)
                .<String, List<StockWeeklyPrice>>chunk(10, platformTransactionManager)
                .reader(stockCodeReader())
                .processor(weeklyStockProcessor())
                .writer(weeklyStockWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<String> stockCodeReader() {
        return new JdbcCursorItemReaderBuilder<String>()
                .name("stockCodeReader")
                .dataSource(dataSource)
                .sql("SELECT DISTINCT stock_code FROM TB_STOCK_PRICE")
                .rowMapper((rs, rowNum) -> rs.getString(1))
                .build();
    }

    @Bean
    public ItemProcessor<String, List<StockWeeklyPrice>> weeklyStockProcessor() {
        return stockCode -> {
            List<StockPrice> dailyPrices = stockPriceRepository.findByStockCodeOrderByBasDtAsc(stockCode);
            if (dailyPrices == null || dailyPrices.isEmpty()) {
                return null;
            }
            return priceCalculationService.calculateWeeklyPrices(dailyPrices);
        };
    }

    @Bean
    public ItemWriter<List<StockWeeklyPrice>> weeklyStockWriter() {
        return items -> {
            for (List<StockWeeklyPrice> weeklyPrices : items) {
                stockWeeklyPriceRepository.saveAll(weeklyPrices);
            }
        };
    }
}
