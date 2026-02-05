package com.stock.price.batchJob;

import com.stock.common.consts.ApplicationConstants;
import com.stock.price.batchJob.itemReader.WeeklyStockCodeItemReader;
import com.stock.price.entity.StockPrice;
import com.stock.price.entity.StockWeeklyPrice;
import com.stock.price.repository.StockPriceRepository;
import com.stock.price.repository.StockWeeklyPriceRepository;
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
public class StockWeeklyPriceBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StockPriceRepository stockPriceRepository;
    private final PriceCalculationService priceCalculationService;
    private final StockWeeklyPriceRepository stockWeeklyPriceRepository;

    @Bean
    @StepScope
    public WeeklyStockCodeItemReader stockCodeReaderWeekly() {
        return new WeeklyStockCodeItemReader(stockPriceRepository);
    }

    @Bean
    public Job weeklyStockDataJob() {
        return new JobBuilder("weeklyStockDataJob", jobRepository)
                .start(weeklyStockDataStep())
                .build();
    }

    @Bean
    public Step weeklyStockDataStep() {
        return new StepBuilder("weeklyStockDataStep", jobRepository)
                .<String, List<StockWeeklyPrice>>chunk(ApplicationConstants.STOCK_PRICE_CHUNK_SIZE, platformTransactionManager)
                .reader(stockCodeReaderWeekly())
                .processor(weeklyStockProcessor())
                .writer(weeklyStockWriter())
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
                if (weeklyPrices != null && !weeklyPrices.isEmpty()) {
                    stockWeeklyPriceRepository.saveAll(weeklyPrices);
                }
            }
        };
    }
}