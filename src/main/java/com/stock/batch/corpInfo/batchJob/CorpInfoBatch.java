package com.stock.batch.corpInfo.batchJob;

import com.stock.batch.corpInfo.batchJob.ItemReader.CorpInfoItemReader;

import com.stock.batch.corpInfo.entity.CorpInfo;
import com.stock.batch.corpInfo.repository.CorpInfoRepository;
import com.stock.batch.corpFinance.service.CorpFinanceService;
import com.stock.batch.corpInfo.service.CorpInfoService;
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
public class CorpInfoBatch {

    private final JobRepository jobRepository;
    private final CorpInfoRepository corpInfoRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final CorpInfoService corpInfoService;


    @Bean
    @StepScope
    public CorpInfoItemReader corpInfoItemReader() {
        return new CorpInfoItemReader(corpInfoService);
    }


    @Bean
    public Job corpDataJob() {
        return new JobBuilder("corpDataJob", jobRepository)
                .start(corpDataStep())
                .build();
    }

    @Bean
    public Step corpDataStep() {
        return new StepBuilder("corpDataStep", jobRepository)
                .<CorpInfo, CorpInfo>chunk(100, platformTransactionManager)
                .reader(corpInfoItemReader())
                .processor(corpDataProcessor())
                .writer(corpItemWriter())
                .build();
    }


    @Bean
    public ItemProcessor<CorpInfo, CorpInfo> corpDataProcessor() {

        return new ItemProcessor<CorpInfo, CorpInfo>() {
            @Override
            public CorpInfo process(CorpInfo item) throws Exception {

                return item;
            }
        };
    }



    @Bean
    public RepositoryItemWriter<CorpInfo> corpItemWriter() {

        return new RepositoryItemWriterBuilder<CorpInfo>()
                .repository(corpInfoRepository)
                .methodName("save")
                .build();
    }

}
