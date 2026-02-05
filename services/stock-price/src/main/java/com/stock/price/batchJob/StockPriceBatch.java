package com.stock.price.batchJob;

import com.stock.common.consts.ApplicationConstants;
import com.stock.price.batchJob.itemReader.AdjustedPriceItemReader;
import com.stock.price.batchJob.itemReader.CorpEventItemReader;
import com.stock.price.batchJob.itemReader.StockIndicatorItemReader;
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


    @Bean
    @StepScope
    public CorpEventItemReader corpEventItemReader() {
        return new CorpEventItemReader(stockPriceRepository);
    }

    @Bean
    @StepScope
    public AdjustedPriceItemReader adjustedPriceItemReader() {
        return new AdjustedPriceItemReader(stockPriceRepository);
    }

    @Bean
    @StepScope
    public StockIndicatorItemReader stockIndicatorItemReader() {
        return new StockIndicatorItemReader(stockPriceRepository);
    }

    @Bean
    @StepScope
    public StockPriceItemReader stockApiItemReader() {
        return new StockPriceItemReader(stockService);
    }

    @Bean
    public Job stockDataJob() {
        return new JobBuilder("stockDataJob", jobRepository)
                .start(stockDataStep())
                .next(corpEventStep())
                .next(calculateAdjPriceStep())
                .next(calculateIndicatorStep())
                .build();
    }

    @Bean
    public Step stockDataStep() {
        return new StepBuilder("stockDataStep", jobRepository)
                .<StockPrice, StockPrice>chunk(ApplicationConstants.STOCK_PRICE_CHUNK_SIZE, platformTransactionManager)
                .reader(stockApiItemReader())
                .processor(stockItemProcessor())
                .writer(stockItemWriter())
                .build();
    }

    @Bean
    public Step corpEventStep() {
        return new StepBuilder("corpEventStep", jobRepository)
                .<String, String>chunk(ApplicationConstants.STOCK_PRICE_CHUNK_SIZE, platformTransactionManager)
                .reader(corpEventItemReader())
                .writer(chunk -> {
                    for (String code : chunk) {
                        try {
                            corpEventService.collectAndSaveEvents(code);
                        } catch (Exception e) {
                            log.error("Failed to collect events for {}: {}", code, e.getMessage());
                        }
                    }
                })
                .build();
    }

    @Bean
    public Step calculateAdjPriceStep() {
        return new StepBuilder("calculateAdjPriceStep", jobRepository)
                .<String, String>chunk(ApplicationConstants.STOCK_PRICE_CHUNK_SIZE, platformTransactionManager)
                .reader(adjustedPriceItemReader())
                .writer(chunk -> {
                    for (String code : chunk) {
                        try {
                            adjustedPriceService.calculateAndSaveAdjustedPrices(code);
                        } catch (Exception e) {
                            log.error("Failed to calculate adjusted price for {}: {}", code, e.getMessage());
                        }
                    }
                })
                .build();
    }

    @Bean
    public Step calculateIndicatorStep() {
        return new StepBuilder("calculateIndicatorStep", jobRepository)
                .<StockPrice, StockPrice>chunk(ApplicationConstants.STOCK_PRICE_CHUNK_SIZE, platformTransactionManager)
                .reader(stockIndicatorItemReader())
                .processor(indicatorItemProcessor())
                .writer(stockItemWriter())
                .build();
    }

    @Bean
    public ItemProcessor<StockPrice, StockPrice> indicatorItemProcessor() {
        return item -> {
            List<StockPrice> history = stockPriceRepository.findTop300ByStockCodeAndBasDtBeforeOrderByBasDtDesc(
                    item.getStockCode(), item.getBasDt());

            if (history.size() < 300) {
                log.warn("Insufficient history data for stock: {}. Required: 300, Found: {}", item.getStockCode(), history.size());
                return null;
            }

            technicalIndicatorService.calculateAndFillIndicators(item, history);
            
            return item;
        };
    }

    @Bean
    public ItemProcessor<StockPrice, StockPrice> stockItemProcessor() {
        return item -> item;
    }

    @Bean
    public RepositoryItemWriter<StockPrice> stockItemWriter() {
        return new RepositoryItemWriterBuilder<StockPrice>()
                .repository(stockPriceRepository)
                .methodName("save")
                .build();
    }
}
