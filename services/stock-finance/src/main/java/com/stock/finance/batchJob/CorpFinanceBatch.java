package com.stock.finance.batchJob;

import com.stock.common.dto.CorpInfoDto;
import com.stock.common.dto.StockPriceDto;
import com.stock.common.enums.ValidationStatus;
import com.stock.finance.batchJob.ItemReader.CorpFinanceItemReader;
import com.stock.finance.entity.CorpFinance;
import com.stock.finance.entity.CorpFinanceIndicator;
import com.stock.finance.repository.CorpFinanceRepository;
import com.stock.finance.client.CorpClient;
import com.stock.finance.client.StockClient;
import com.stock.finance.service.CorpFinanceService;
import com.stock.finance.service.FinanceValidationService;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@AllArgsConstructor
public class CorpFinanceBatch {

    private final JobRepository jobRepository;
    private final CorpFinanceRepository corpFinanceRepository;
    private final CorpClient corpClient;
    private final StockClient stockClient;
    private final PlatformTransactionManager platformTransactionManager;
    private final CorpFinanceService corpFinanceService;
    private final FinanceValidationService financeValidationService;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public CorpFinanceItemReader corpFinanceItemReader() {
        return new CorpFinanceItemReader(corpFinanceService, corpClient);
    }

    @Bean
    public Job corpFinanceJob() {
        return new JobBuilder("corpFinanceJob", jobRepository)
                .start(corpFinanceStep())
                .next(validateFinanceStep())
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
    public Step validateFinanceStep() {
        return new StepBuilder("validateFinanceStep", jobRepository)
                .<CorpFinance, CorpFinance>chunk(100, platformTransactionManager)
                .reader(validateFinanceItemReader())
                .processor(validateFinanceProcessor())
                .writer(corpFinanceWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<CorpFinance> validateFinanceItemReader() {
        return new JpaPagingItemReaderBuilder<CorpFinance>()
                .name("validateFinanceItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT f FROM CorpFinance f WHERE f.validationStatus IS NULL")
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<CorpFinance, CorpFinance> validateFinanceProcessor() {
        return item -> {
            ValidationStatus status = financeValidationService.performValidation(item);
            item.setValidationStatus(status);
            return item;
        };
    }

    @Bean
    public ItemProcessor<CorpFinance, CorpFinance> corpFinanceProcessor() {
        return item -> {
            CorpInfoDto corpInfo = corpClient.getCorpInfo(item.getCorpCode());
            if (corpInfo != null && corpInfo.getStockCode() != null) {
                StockPriceDto stockPrice = stockClient.getLatestStockPrice(corpInfo.getStockCode());

                if (stockPrice != null) {
                    CorpFinanceIndicator indicator = corpFinanceService.calculateIndicators(item, stockPrice.getMarketTotalAmt());
                    item.setCorpFinanceIndicator(indicator);
                    indicator.setCorpFinance(item);
                }
            }
            return item;
        };
    }


    @Bean
    public RepositoryItemWriter<CorpFinance> corpFinanceWriter() {
        return new RepositoryItemWriterBuilder<CorpFinance>()
                .repository(corpFinanceRepository)
                .methodName("save")
                .build();
    }
}
