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
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public TaskExecutor weeklyBatchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("WeeklyBatch-Thread-");
        executor.initialize();
        return executor;
    }

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
                .taskExecutor(weeklyBatchTaskExecutor())
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
                    String stockCode = weeklyPrices.get(0).getStockCode();
                    
                    // 해당 종목의 기존 데이터를 한 번에 로드 (전체 조회가 아닌 종목 단위로 최적화)
                    Map<String, StockWeeklyPrice> existingMap = new HashMap<>();
                    stockWeeklyPriceRepository.findByStockCodeInAndEndDateBetween(
                            List.of(stockCode), LocalDate.of(1900,1,1), LocalDate.of(2099,12,31))
                            .forEach(p -> existingMap.put(p.getStartDate() + "_" + p.getEndDate(), p));

                    for (StockWeeklyPrice newPrice : weeklyPrices) {
                        String key = newPrice.getStartDate() + "_" + newPrice.getEndDate();
                        StockWeeklyPrice existing = existingMap.get(key);
                        
                        if (existing != null) {
                            updateWeeklyPriceData(existing, newPrice);
                            stockWeeklyPriceRepository.save(existing);
                        } else {
                            stockWeeklyPriceRepository.save(newPrice);
                        }
                    }
                }
            }
        };
    }

    private void updateWeeklyPriceData(StockWeeklyPrice existing, StockWeeklyPrice newData) {
        existing.setMarketCode(newData.getMarketCode());
        existing.setStartPrice(newData.getStartPrice());
        existing.setEndPrice(newData.getEndPrice());
        existing.setHighPrice(newData.getHighPrice());
        existing.setLowPrice(newData.getLowPrice());
        existing.setVolume(newData.getVolume());
        existing.setVolumePrice(newData.getVolumePrice());
        existing.setStockTotalCnt(newData.getStockTotalCnt());
        existing.setMarketTotalAmt(newData.getMarketTotalAmt());
    }
}