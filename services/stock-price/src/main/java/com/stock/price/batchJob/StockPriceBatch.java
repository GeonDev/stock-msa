package com.stock.price.batchJob;

import com.stock.price.batchJob.itemReader.StockPriceItemReader;
import com.stock.price.entity.StockPrice;
import com.stock.price.repository.StockPriceRepository;
import com.stock.price.service.AdjustedPriceService;
import com.stock.price.service.CorpEventService;
import com.stock.price.service.StockService;
import com.stock.price.service.TechnicalIndicatorService;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockPriceBatch {

    private final JobRepository jobRepository;
    private final StockPriceRepository stockPriceRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StockService stockService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final CorpEventService corpEventService;
    private final AdjustedPriceService adjustedPriceService;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public StockPriceItemReader stockApiItemReader() {
        return new StockPriceItemReader(stockService);
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<StockPrice> stockIndicatorItemReader(@Value("#{jobParameters['targetDate']}") String targetDate) {
        LocalDate date = (targetDate != null) ? LocalDate.parse(targetDate) : LocalDate.now();
        
        return new JpaPagingItemReaderBuilder<StockPrice>()
                .name("stockIndicatorItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s FROM StockPrice s WHERE s.basDt = :targetDate")
                .parameterValues(Collections.singletonMap("targetDate", date))
                .pageSize(100)
                .build();
    }

    @Bean
    public Job stockDataJob() {
        return new JobBuilder("stockDataJob", jobRepository)
                .start(stockDataStep())
                .next(corpEventStep(null))
                .next(calculateAdjPriceStep(null))
                .next(calculateIndicatorStep())
                .build();
    }

    @Bean
    public Step stockDataStep() {
        return new StepBuilder("stockDataStep", jobRepository)
                .<StockPrice, StockPrice>chunk(100, platformTransactionManager)
                .reader(stockApiItemReader())
                .processor(stockItemProcessor()) // Simple pass-through
                .writer(stockItemWriter())
                .build();
    }

    @Bean
    @StepScope
    public Step corpEventStep(@Value("#{jobParameters['targetDate']}") String targetDate) {
        return new StepBuilder("corpEventStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate date = (targetDate != null) ? LocalDate.parse(targetDate) : LocalDate.now();
                    log.info("Starting Corp Event Collection for date: {}", date);
                    
                    // 1. 금일 수집된 종목 리스트 조회
                    List<String> stockCodes = stockPriceRepository.findDistinctStockCodeByBasDtBetween(date, date);
                    
                    for (String code : stockCodes) {
                        try {
                            corpEventService.collectAndSaveEvents(code);
                        } catch (Exception e) {
                            log.error("Failed to collect events for {}: {}", code, e.getMessage());
                        }
                    }
                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Step calculateAdjPriceStep(@Value("#{jobParameters['targetDate']}") String targetDate) {
        return new StepBuilder("calculateAdjPriceStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate date = (targetDate != null) ? LocalDate.parse(targetDate) : LocalDate.now();
                    log.info("Starting Adjusted Price Calculation for date: {}", date);
                    
                    List<String> stockCodes = stockPriceRepository.findDistinctStockCodeByBasDtBetween(date, date);
                    
                    for (String code : stockCodes) {
                         try {
                            adjustedPriceService.calculateAndSaveAdjustedPrices(code);
                        } catch (Exception e) {
                            log.error("Failed to calculate adjusted price for {}: {}", code, e.getMessage());
                        }
                    }
                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }

    @Bean
    public Step calculateIndicatorStep() {
        return new StepBuilder("calculateIndicatorStep", jobRepository)
                .<StockPrice, StockPrice>chunk(100, platformTransactionManager)
                .reader(stockIndicatorItemReader(null)) // null for compile-time, overridden by StepScope
                .processor(indicatorItemProcessor())
                .writer(stockItemWriter())
                .build();
    }

    @Bean
    public ItemProcessor<StockPrice, StockPrice> indicatorItemProcessor() {
        return item -> {
            // 1. Fetch historical data (Top 300 for MA250 calculation)
            List<StockPrice> history = stockPriceRepository.findTop300ByStockCodeAndBasDtBeforeOrderByBasDtDesc(
                    item.getStockCode(), item.getBasDt());
            
            // 2. Calculate indicators
            technicalIndicatorService.calculateAndFillIndicators(item, history);
            
            return item;
        };
    }

    @Bean
    public ItemProcessor<StockPrice, StockPrice> stockItemProcessor() {
        return item -> item; // Data collection only
    }

    @Bean
    public RepositoryItemWriter<StockPrice> stockItemWriter() {
        return new RepositoryItemWriterBuilder<StockPrice>()
                .repository(stockPriceRepository)
                .methodName("save")
                .build();
    }
}
