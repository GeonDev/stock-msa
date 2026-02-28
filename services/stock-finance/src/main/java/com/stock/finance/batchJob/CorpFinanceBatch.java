package com.stock.finance.batchJob;

import com.stock.common.dto.CorpInfoDto;
import com.stock.common.dto.StockPriceDto;
import com.stock.common.enums.ValidationStatus;
import com.stock.finance.batchJob.ItemReader.CorpFinanceItemReader;
import com.stock.finance.batchJob.ItemReader.ValidateFinanceItemReader;
import com.stock.finance.client.CorpClient;
import com.stock.finance.entity.CorpFinance;
import com.stock.finance.entity.CorpFinanceIndicator;
import com.stock.finance.repository.CorpFinanceRepository;
import com.stock.finance.client.StockClient;
import com.stock.finance.service.CorpFinanceService;
import com.stock.finance.service.FinanceValidationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.stock.common.utils.DateUtils;

import static com.stock.common.consts.ApplicationConstants.STOCK_FINANCE_CHUNK_SIZE;

@Slf4j
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
    private final CacheManager cacheManager;

    @Bean
    @StepScope
    public CorpFinanceItemReader corpFinanceItemReader() {
        return new CorpFinanceItemReader(corpFinanceService, corpClient);
    }

    @Bean
    public ValidateFinanceItemReader validateFinanceItemReader() {
        return new ValidateFinanceItemReader(corpFinanceRepository);
    }

    @Bean
    public Job corpFinanceJob() {
        return new JobBuilder("corpFinanceJob", jobRepository)
                .start(corpFinanceStep())
                .next(validateFinanceStep())
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            cacheManager.getCache("financeCache").clear();
                            cacheManager.getCache("indicatorCache").clear();
                            // 전략 서비스의 유니버스 캐시도 함께 제거 (공용 레디스 사용)
                            if (cacheManager.getCache("universeCache") != null) {
                                cacheManager.getCache("universeCache").clear();
                            }
                            log.info("Finance and Universe caches evicted after corpFinanceJob completion");
                        }
                    }
                })
                .build();
    }

    @Bean
    public Step corpFinanceStep() {
        return new StepBuilder("corpFinanceStep", jobRepository)
                .<CorpFinance, CorpFinance>chunk(STOCK_FINANCE_CHUNK_SIZE, platformTransactionManager)
                .reader(corpFinanceItemReader())
                .processor(corpFinanceProcessor(null))
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
    public ItemProcessor<CorpFinance, CorpFinance> validateFinanceProcessor() {
        return item -> {
            ValidationStatus status = financeValidationService.performValidation(item);
            item.setValidationStatus(status);
            return item;
        };
    }

    @Bean
    @StepScope
    public ItemProcessor<CorpFinance, CorpFinance> corpFinanceProcessor(
            @Value("#{jobParameters['date']}") String jobDate) {
        
        return new ItemProcessor<CorpFinance, CorpFinance>() {
            private Map<String, CorpFinance> existingMap = null;

            @Override
            public CorpFinance process(CorpFinance item) {
                if (existingMap == null) {
                    existingMap = new HashMap<>();
                    LocalDate date = DateUtils.toStringLocalDate(jobDate);
                    String bizYear = String.valueOf(date.getYear());
                    
                    List<CorpFinance> existingList = corpFinanceRepository.findByBizYear(bizYear);
                    for (CorpFinance f : existingList) {
                        existingMap.put(f.getCorpCode() + "_" + f.getReportCode(), f);
                    }
                }

                CorpFinance existing = existingMap.get(item.getCorpCode() + "_" + item.getReportCode());

                if (existing != null) {
                    updateFinanceData(existing, item);
                    item = existing;
                }

                try {
                    // corpCode 필드는 실제로 stockCode를 저장 (A005930 형식)
                    String stockCode = item.getCorpCode().replace("A", "");
                    StockPriceDto stockPrice = stockClient.getLatestStockPrice(stockCode);

                    if (stockPrice != null) {
                        CorpFinanceIndicator indicator = corpFinanceService.calculateIndicators(item, stockPrice.getMarketTotalAmt());
                        
                        if (item.getCorpFinanceIndicator() != null) {
                            updateIndicatorData(item.getCorpFinanceIndicator(), indicator);
                        } else {
                            item.setCorpFinanceIndicator(indicator);
                            indicator.setCorpFinance(item);
                        }
                    }
                } catch (Exception e) {
                    // 재무 지표 계산 실패 시에도 재무 데이터는 저장
                    log.warn("Finance Indicator fail");
                }
                return item;
            }
        };
    }

    private void updateFinanceData(CorpFinance existing, CorpFinance newData) {
        existing.setBasDt(newData.getBasDt());
        existing.setCurrency(newData.getCurrency());
        existing.setOpIncome(newData.getOpIncome());
        existing.setNetIncome(newData.getNetIncome());
        existing.setRevenue(newData.getRevenue());
        existing.setTotalAsset(newData.getTotalAsset());
        existing.setTotalDebt(newData.getTotalDebt());
        existing.setTotalCapital(newData.getTotalCapital());
        existing.setOperatingCashflow(newData.getOperatingCashflow());
        existing.setInvestingCashflow(newData.getInvestingCashflow());
        existing.setFinancingCashflow(newData.getFinancingCashflow());
        existing.setFreeCashflow(newData.getFreeCashflow());
        existing.setEbitda(newData.getEbitda());
        existing.setDepreciation(newData.getDepreciation());
    }

    private void updateIndicatorData(CorpFinanceIndicator existing, CorpFinanceIndicator newData) {
        existing.setRoe(newData.getRoe());
        existing.setRoa(newData.getRoa());
        existing.setPer(newData.getPer());
        existing.setPbr(newData.getPbr());
        existing.setPsr(newData.getPsr());
        existing.setPcr(newData.getPcr());
        existing.setFcfYield(newData.getFcfYield());
        existing.setEvEbitda(newData.getEvEbitda());
        existing.setOperatingMargin(newData.getOperatingMargin());
        existing.setNetMargin(newData.getNetMargin());
        existing.setQoqRevenueGrowth(newData.getQoqRevenueGrowth());
        existing.setQoqOpIncomeGrowth(newData.getQoqOpIncomeGrowth());
        existing.setQoqNetIncomeGrowth(newData.getQoqNetIncomeGrowth());
        existing.setYoyRevenueGrowth(newData.getYoyRevenueGrowth());
        existing.setYoyOpIncomeGrowth(newData.getYoyOpIncomeGrowth());
        existing.setYoyNetIncomeGrowth(newData.getYoyNetIncomeGrowth());
    }


    @Bean
    public RepositoryItemWriter<CorpFinance> corpFinanceWriter() {
        return new RepositoryItemWriterBuilder<CorpFinance>()
                .repository(corpFinanceRepository)
                .methodName("save")
                .build();
    }
}
