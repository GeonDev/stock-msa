package com.stock.price.batchJob;

import com.stock.common.consts.ApplicationConstants;
import com.stock.price.batchJob.itemReader.AdjustedPriceItemReader;
import com.stock.price.batchJob.itemReader.CorpEventItemReader;
import com.stock.price.batchJob.itemReader.StockIndicatorItemReader;
import com.stock.price.batchJob.itemReader.StockPriceItemReader;
import com.stock.price.entity.StockPrice;
import com.stock.price.repository.StockPriceRepository;
import com.stock.price.service.*;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.stock.common.utils.DateUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockPriceBatch {

    private final JobRepository jobRepository;
    private final StockPriceRepository stockPriceRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StockService stockService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final MarketCapRankService marketCapRankService;
    private final CorpEventService corpEventService;
    private final AdjustedPriceService adjustedPriceService;
    private final CacheManager cacheManager;

    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Batch-Thread-");
        executor.initialize();
        return executor;
    }


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
                .next(calculateRankStep())
                .next(corpEventStep())
                .next(calculateAdjPriceStep())
                .next(calculateIndicatorStep())
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            cacheManager.getCache("priceCache").clear();
                            cacheManager.getCache("latestPriceCache").clear();
                            cacheManager.getCache("historyPriceCache").clear();
                            // 전략 서비스의 유니버스 캐시도 함께 제거 (공용 레디스 사용)
                            if (cacheManager.getCache("universeCache") != null) {
                                cacheManager.getCache("universeCache").clear();
                            }
                            log.info("Price and Universe caches evicted after stockDataJob completion");
                        }
                    }
                })
                .build();
    }

    @Bean
    public Job stockPriceRecoveryJob(Step weeklyStockDataStep, Step monthlyStockDataStep) {
        return new JobBuilder("stockPriceRecoveryJob", jobRepository)
                .start(stockDataStep()) // 1. 일별 데이터 수집/업데이트
                .next(calculateRankStep())
                .next(weeklyStockDataStep) // 2. 주간 데이터 집계/업데이트
                .next(monthlyStockDataStep) // 3. 월간 데이터 집계/업데이트
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            cacheManager.getCache("priceCache").clear();
                            cacheManager.getCache("latestPriceCache").clear();
                            cacheManager.getCache("historyPriceCache").clear();
                            if (cacheManager.getCache("universeCache") != null) {
                                cacheManager.getCache("universeCache").clear();
                            }
                            log.info("Price and Universe caches evicted after stockPriceRecoveryJob completion");
                        }
                    }
                })
                .build();
    }

    @Bean
    public Step stockDataStep() {
        return new StepBuilder("stockDataStep", jobRepository)
                .<StockPrice, StockPrice>chunk(ApplicationConstants.STOCK_PRICE_CHUNK_SIZE, platformTransactionManager)
                .reader(stockApiItemReader())
                .processor(stockItemProcessor(null))
                .writer(stockItemWriter())
                .taskExecutor(batchTaskExecutor())
                .build();
    }

    @Bean
    public Step calculateRankStep() {
        return new StepBuilder("calculateRankStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String jobDate = (String) chunkContext.getStepContext().getJobParameters().get("date");
                    LocalDate date = DateUtils.toStringLocalDate(jobDate);
                    marketCapRankService.calculateAndSaveMarketCapRanks(date);
                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
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
                .taskExecutor(batchTaskExecutor())
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
                .taskExecutor(batchTaskExecutor())
                .build();
    }

    @Bean
    public Step calculateIndicatorStep() {
        return new StepBuilder("calculateIndicatorStep", jobRepository)
                .<StockPrice, StockPrice>chunk(ApplicationConstants.STOCK_PRICE_CHUNK_SIZE, platformTransactionManager)
                .reader(stockIndicatorItemReader())
                .processor(indicatorItemProcessor())
                .writer(stockItemWriter())
                .taskExecutor(batchTaskExecutor())
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
    @StepScope
    public ItemProcessor<StockPrice, StockPrice> stockItemProcessor(
            @Value("#{jobParameters['date']}") String jobDate) {
        
        return new ItemProcessor<StockPrice, StockPrice>() {
            private Map<String, StockPrice> existingMap = null;

            @Override
            public StockPrice process(StockPrice item) {
                if (existingMap == null) {
                    existingMap = new HashMap<>();
                    LocalDate date = DateUtils.toStringLocalDate(jobDate);
                    // 해당 날짜의 전체 시세 데이터를 한 번에 로드
                    List<StockPrice> existingList = stockPriceRepository.findByBasDt(date);
                    for (StockPrice p : existingList) {
                        existingMap.put(p.getStockCode(), p);
                    }
                    log.info("Pre-loaded {} existing prices for date {}", existingMap.size(), jobDate);
                }

                // 메모리 맵에서 즉시 조회
                StockPrice existing = existingMap.get(item.getStockCode());
                if (existing != null) {
                    updateStockPriceData(existing, item);
                    return existing;
                }
                return item;
            }
        };
    }

    private void updateStockPriceData(StockPrice existing, StockPrice newData) {
        existing.setMarketCode(newData.getMarketCode());
        existing.setAdjClosePrice(newData.getAdjClosePrice());
        existing.setVolume(newData.getVolume());
        existing.setVolumePrice(newData.getVolumePrice());
        existing.setStartPrice(newData.getStartPrice());
        existing.setEndPrice(newData.getEndPrice());
        existing.setHighPrice(newData.getHighPrice());
        existing.setLowPrice(newData.getLowPrice());
        existing.setDailyRange(newData.getDailyRange());
        existing.setDailyRatio(newData.getDailyRatio());
        existing.setStockTotalCnt(newData.getStockTotalCnt());
        existing.setMarketTotalAmt(newData.getMarketTotalAmt());
    }

    @Bean
    public RepositoryItemWriter<StockPrice> stockItemWriter() {
        return new RepositoryItemWriterBuilder<StockPrice>()
                .repository(stockPriceRepository)
                .methodName("save")
                .build();
    }
}
