package com.stock.batch.batchJob;

import com.stock.batch.batchJob.ItemReader.StockApiItemReader;
import com.stock.batch.entity.AfterEntity;
import com.stock.batch.entity.BeforeEntity;
import com.stock.batch.entity.StockPrice;
import com.stock.batch.entity.WinEntity;
import com.stock.batch.repository.StockPriceRepository;
import com.stock.batch.repository.WinRepository;
import com.stock.batch.service.StockApiService;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.Map;

@Configuration
@AllArgsConstructor
public class StockPriceBatch {

    private final JobRepository jobRepository;
    private final StockPriceRepository stockPriceRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StockApiService stockApiService;


    @Bean
    @StepScope
    public StockApiItemReader stockApiItemReader() {
        return new StockApiItemReader(stockApiService);
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
                .writer(stockItemWriter())
                .build();
    }

    @Bean
    public RepositoryItemWriter<StockPrice> stockItemWriter() {

        return new RepositoryItemWriterBuilder<StockPrice>()
                .repository(stockPriceRepository)
                .methodName("saveAll")
                .build();
    }

}
