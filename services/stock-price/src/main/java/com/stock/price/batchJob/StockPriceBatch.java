package com.stock.price.batchJob;

import com.stock.price.batchJob.itemReader.StockPriceItemReader;
import com.stock.price.entity.StockIndicator;
import com.stock.price.entity.StockPrice;
import com.stock.price.repository.StockPriceRepository;
import com.stock.price.service.StockService;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@AllArgsConstructor
public class StockPriceBatch {

    private final JobRepository jobRepository;
    private final StockPriceRepository stockPriceRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StockService stockService;


    @Bean
    @StepScope
    public StockPriceItemReader stockApiItemReader() {
        return new StockPriceItemReader(stockService);
    }


    @Bean
    public Job stockDataJob() {
        return new JobBuilder("stockDataJob", jobRepository)
                .start(stockDataStep())
                .build();
    }

    @Bean
    public Step stockDataStep() {
        return new StepBuilder("stockDataStep", jobRepository)
                .<StockPrice, StockPrice>chunk(100, platformTransactionManager)
                .reader(stockApiItemReader())
                .processor(stockItemProcessor())
                .writer(stockItemWriter())
                .build();
    }

    @Bean
    public ItemProcessor<StockPrice, StockPrice> stockItemProcessor() {
        return item -> {
            List<StockPrice> historicalPrices = stockPriceRepository.findTop200ByStockCodeAndBasDtBeforeOrderByBasDtDesc(item.getStockCode(), item.getBasDt());

            if (historicalPrices != null && !historicalPrices.isEmpty()) {
                StockIndicator indicator = new StockIndicator();
                indicator.setMa5(calculateMovingAverage(historicalPrices, 5));
                indicator.setMa20(calculateMovingAverage(historicalPrices, 20));
                indicator.setMa60(calculateMovingAverage(historicalPrices, 60));
                indicator.setMa120(calculateMovingAverage(historicalPrices, 120));
                indicator.setMa200(calculateMovingAverage(historicalPrices, 200));
                indicator.setMa250(calculateMovingAverage(historicalPrices, 250));

                indicator.setMomentum1m(calculateMomentum(item.getEndPrice(), historicalPrices, 1));
                indicator.setMomentum3m(calculateMomentum(item.getEndPrice(), historicalPrices, 3));
                indicator.setMomentum6m(calculateMomentum(item.getEndPrice(), historicalPrices, 6));

                item.setStockIndicator(indicator);
                indicator.setStockPrice(item);
            }
            return item;
        };
    }

    private Double calculateMovingAverage(List<StockPrice> prices, int days) {
        if (prices.size() < days) {
            return null; // Not enough data
        }
        return prices.stream()
                .limit(days)
                .mapToInt(StockPrice::getEndPrice)
                .average()
                .orElse(0.0);
    }

    private Double calculateMomentum(Integer currentPrice, List<StockPrice> prices, int months) {
        if (prices.size() < (long) months * 20) { // Approximate trading days in a month
            return null;
        }
        Integer pastPrice = prices.get(months * 20 -1).getEndPrice();
        if (pastPrice == null || pastPrice == 0) {
            return 0.0;
        }
        return ((double) currentPrice / pastPrice - 1) * 100;
    }


    @Bean
    public RepositoryItemWriter<StockPrice> stockItemWriter() {

        return new RepositoryItemWriterBuilder<StockPrice>()
                .repository(stockPriceRepository)
                .methodName("save")
                .build();
    }

}
