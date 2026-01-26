package com.stock.batch.batchJob;

import com.stock.batch.batchJob.ItemReader.CorpFinanceItemReader;
import com.stock.batch.entity.CorpFinance;
import com.stock.batch.entity.CorpFinanceIndicator;
import com.stock.batch.entity.CorpInfo;
import com.stock.batch.entity.StockPrice;
import com.stock.batch.repository.CorpFinanceRepository;
import com.stock.batch.repository.CorpInfoRepository;
import com.stock.batch.repository.StockPriceRepository;
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

import java.util.Optional;

@Configuration
@AllArgsConstructor
public class CorpFinanceBatch {

    private final JobRepository jobRepository;
    private final CorpFinanceRepository corpFinanceRepository;
    private final CorpInfoRepository corpInfoRepository;
    private final StockPriceRepository stockPriceRepository;
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
        return item -> {
            Optional<CorpInfo> corpInfoOpt = corpInfoRepository.findById(item.getCorpCode());
            if (corpInfoOpt.isPresent() && corpInfoOpt.get().getStockCode() != null) {
                CorpInfo corpInfo = corpInfoOpt.get();
                Optional<StockPrice> stockPriceOpt = stockPriceRepository.findFirstByStockCodeOrderByBasDtDesc(corpInfo.getStockCode());

                if (stockPriceOpt.isPresent()) {
                    StockPrice stockPrice = stockPriceOpt.get();
                    CorpFinanceIndicator indicator = calculateIndicators(item, stockPrice.getMarketTotalAmt());
                    item.setCorpFinanceIndicator(indicator);
                    indicator.setCorpFinance(item);
                }
            }
            return item;
        };
    }

    private CorpFinanceIndicator calculateIndicators(CorpFinance currentFinance, Long marketCap) {
        CorpFinanceIndicator.CorpFinanceIndicatorBuilder builder = CorpFinanceIndicator.builder()
                .corpCode(currentFinance.getCorpCode())
                .basDt(currentFinance.getBasDt());

        // Calculate ROE, ROA, Debt Ratio from current data
        if (currentFinance.getTotalCapital() != null && currentFinance.getTotalCapital() != 0) {
            if (currentFinance.getNetIncome() != null) {
                builder.roe((double) currentFinance.getNetIncome() / currentFinance.getTotalCapital() * 100);
            }
        }
        if (currentFinance.getTotalAsset() != null && currentFinance.getTotalAsset() != 0 && currentFinance.getNetIncome() != null) {
            builder.roa((double) currentFinance.getNetIncome() / currentFinance.getTotalAsset() * 100);
        }

        // Calculate PER, PBR, PSR from current data and market cap
        if (marketCap != null && marketCap > 0) {
            if (currentFinance.getNetIncome() != null && currentFinance.getNetIncome() > 0) {
                builder.per((double) marketCap / currentFinance.getNetIncome());
            }
            if (currentFinance.getTotalCapital() != null && currentFinance.getTotalCapital() > 0) {
                builder.pbr((double) marketCap / currentFinance.getTotalCapital());
            }
            if (currentFinance.getRevenue() != null && currentFinance.getRevenue() > 0) {
                builder.psr((double) marketCap / currentFinance.getRevenue());
            }
        }

        // Fetch previous finance data to calculate growth rates
        Optional<CorpFinance> prevFinanceOpt = corpFinanceRepository.findTop1ByCorpCodeAndBasDtBeforeOrderByBasDtDesc(currentFinance.getCorpCode(), currentFinance.getBasDt());
        if (prevFinanceOpt.isPresent()) {
            CorpFinance prevFinance = prevFinanceOpt.get();
            builder.revenueGrowth(calculateGrowthRate(currentFinance.getRevenue(), prevFinance.getRevenue()));
            builder.netIncomeGrowth(calculateGrowthRate(currentFinance.getNetIncome(), prevFinance.getNetIncome()));
            builder.opIncomeGrowth(calculateGrowthRate(currentFinance.getOpIncome(), prevFinance.getOpIncome()));
        }

        return builder.build();
    }

    private Double calculateGrowthRate(Long currentValue, Long previousValue) {
        if (currentValue == null || previousValue == null || previousValue == 0) {
            return null;
        }
        return ((double) currentValue / previousValue - 1) * 100;
    }

    @Bean
    public RepositoryItemWriter<CorpFinance> corpFinanceWriter() {
        return new RepositoryItemWriterBuilder<CorpFinance>()
                .repository(corpFinanceRepository)
                .methodName("save")
                .build();
    }
}
