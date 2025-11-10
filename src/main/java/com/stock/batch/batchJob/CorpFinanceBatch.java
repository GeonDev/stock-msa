package com.stock.batch.batchJob;

import com.stock.batch.batchJob.ItemReader.CorpFinanceItemReader;
import com.stock.batch.entity.CorpFinance;
import com.stock.batch.entity.CorpInfo;
import com.stock.batch.repository.CorpFinanceRepository;

import com.stock.batch.service.StockApiService;
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

@Configuration
@AllArgsConstructor
public class CorpFinanceBatch {

    private final JobRepository jobRepository;
    private final CorpFinanceRepository  corpFinanceRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StockApiService stockApiService;


    @Bean
    @StepScope
    public CorpFinanceItemReader corpFinanceItemReader() {
        return new CorpFinanceItemReader(stockApiService);
    }


    @Bean
    public Job corpFinanceJob() {
        return new JobBuilder("corpFinanceJob", jobRepository)
                .start(corpFinanceStep())
                .build();
    }

    @Bean
    public Step corpFinanceStep() {
        return new StepBuilder("corpFinanceStep", jobRepository)
                .<CorpFinance, CorpFinance>chunk(1000, platformTransactionManager)
                .reader(corpFinanceItemReader())
                .processor(corpFinanceProcessor())
                .writer(corpFinanceWriter())
                .build();
    }


    @Bean
    public ItemProcessor<CorpFinance, CorpFinance> corpFinanceProcessor() {

        return new ItemProcessor<CorpFinance, CorpFinance>() {
            @Override
            public CorpFinance process(CorpFinance item) throws Exception {

                return item;
            }
        };
    }



    @Bean
    public RepositoryItemWriter<CorpFinance> corpFinanceWriter() {

        return new RepositoryItemWriterBuilder<CorpFinance>()
                .repository(corpFinanceRepository)
                .methodName("saveAll")
                .build();
    }

}
