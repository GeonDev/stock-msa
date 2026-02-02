package com.stock.finance.batchJob;

import com.stock.finance.batchJob.ItemReader.CorpFinanceItemReader;
import com.stock.finance.entity.CorpFinance;
import com.stock.finance.entity.CorpFinanceIndicator;
import com.stock.finance.dto.CorpInfoDto;
import com.stock.finance.dto.StockPriceDto;
import com.stock.finance.repository.CorpFinanceRepository;
import com.stock.finance.client.CorpClient;
import com.stock.finance.client.StockClient;
import com.stock.finance.service.CorpFinanceService;
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
    private final CorpFinanceRepository corpFinanceRepository;
    private final CorpClient corpClient;
    private final StockClient stockClient;
    private final PlatformTransactionManager platformTransactionManager;
    private final CorpFinanceService corpFinanceService;

    @Bean
    @StepScope
    public CorpFinanceItemReader corpFinanceItemReader() {
        return new CorpFinanceItemReader(corpFinanceService, corpClient);
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
