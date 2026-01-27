package com.stock.batch.corpFinance.batchJob;

import com.stock.batch.corpFinance.batchJob.ItemReader.CorpFinanceItemReader;
import com.stock.batch.corpFinance.entity.CorpFinance;
import com.stock.batch.corpFinance.entity.CorpFinanceIndicator;
import com.stock.batch.corpInfo.entity.CorpInfo;
import com.stock.batch.stock.entity.StockPrice;
import com.stock.batch.corpFinance.repository.CorpFinanceRepository;
import com.stock.batch.corpInfo.repository.CorpInfoRepository;
import com.stock.batch.stock.repository.StockPriceRepository;
import com.stock.batch.corpFinance.service.CorpFinanceService;
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

import java.util.Optional;

@Configuration
@AllArgsConstructor
public class CorpFinanceBatch {

    private final JobRepository jobRepository;
    private final CorpFinanceRepository corpFinanceRepository;
    private final CorpInfoRepository corpInfoRepository;
    private final StockPriceRepository stockPriceRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final CorpFinanceService corpFinanceService;

    @Bean
    @StepScope
    public CorpFinanceItemReader corpFinanceItemReader() {
        return new CorpFinanceItemReader(corpFinanceService, corpInfoRepository);
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
            Optional<CorpInfo> corpInfoOpt = corpInfoRepository.findById(item.getCorpCode());
            if (corpInfoOpt.isPresent() && corpInfoOpt.get().getStockCode() != null) {
                CorpInfo corpInfo = corpInfoOpt.get();
                Optional<StockPrice> stockPriceOpt = stockPriceRepository.findFirstByStockCodeOrderByBasDtDesc(corpInfo.getStockCode());

                if (stockPriceOpt.isPresent()) {
                    StockPrice stockPrice = stockPriceOpt.get();
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
