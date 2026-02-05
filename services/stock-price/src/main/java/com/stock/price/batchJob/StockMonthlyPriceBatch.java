package com.stock.price.batchJob;

import com.stock.common.consts.ApplicationConstants;
import com.stock.price.batchJob.itemReader.MonthlyStockCodeItemReader;
import com.stock.price.entity.StockMonthlyPrice;
import com.stock.price.entity.StockPrice;
import com.stock.price.repository.StockMonthlyPriceRepository;
import com.stock.price.repository.StockPriceRepository;
import com.stock.price.service.PriceCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockMonthlyPriceBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StockPriceRepository stockPriceRepository;
    private final PriceCalculationService priceCalculationService;
    private final StockMonthlyPriceRepository stockMonthlyPriceRepository;

    @Bean
    @StepScope
    public MonthlyStockCodeItemReader stockCodeReaderMonthly() {
        return new MonthlyStockCodeItemReader(stockPriceRepository);
    }

    @Bean
    public Job monthlyStockDataJob() {
        return new JobBuilder("monthlyStockDataJob", jobRepository)
                .start(monthlyStockDataStep())
                .build();
    }

    @Bean
    public Step monthlyStockDataStep() {
        return new StepBuilder("monthlyStockDataStep", jobRepository)
                .<String, List<StockMonthlyPrice>>chunk(ApplicationConstants.STOCK_PRICE_CHUNK_SIZE, platformTransactionManager)
                .reader(stockCodeReaderMonthly())
                .processor(monthlyStockProcessor())
                .writer(monthlyStockWriter())
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
                if (monthlyPrices != null && !monthlyPrices.isEmpty()) {
                    stockMonthlyPriceRepository.saveAll(monthlyPrices);
                }
            }
        };
    }
}