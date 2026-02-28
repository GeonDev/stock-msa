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
public class StockMonthlyPriceBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StockPriceRepository stockPriceRepository;
    private final PriceCalculationService priceCalculationService;
    private final StockMonthlyPriceRepository stockMonthlyPriceRepository;

    @Bean
    public TaskExecutor monthlyBatchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("MonthlyBatch-Thread-");
        executor.initialize();
        return executor;
    }

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
                .taskExecutor(monthlyBatchTaskExecutor())
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
                    String stockCode = monthlyPrices.get(0).getStockCode();

                    // 해당 종목의 기존 데이터를 한 번에 로드
                    Map<String, StockMonthlyPrice> existingMap = new HashMap<>();
                    stockMonthlyPriceRepository.findByStockCodeInAndEndDateBetween(
                            List.of(stockCode), LocalDate.of(1900, 1, 1), LocalDate.of(2099, 12, 31))
                            .forEach(p -> existingMap.put(p.getStartDate() + "_" + p.getEndDate(), p));

                    for (StockMonthlyPrice newPrice : monthlyPrices) {
                        String key = newPrice.getStartDate() + "_" + newPrice.getEndDate();
                        StockMonthlyPrice existing = existingMap.get(key);

                        if (existing != null) {
                            updateMonthlyPriceData(existing, newPrice);
                            stockMonthlyPriceRepository.save(existing);
                        } else {
                            stockMonthlyPriceRepository.save(newPrice);
                        }
                    }
                }
            }
        };
    }

    private void updateMonthlyPriceData(StockMonthlyPrice existing, StockMonthlyPrice newData) {
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