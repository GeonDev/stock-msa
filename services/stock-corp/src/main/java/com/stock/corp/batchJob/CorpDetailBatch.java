package com.stock.corp.batchJob;

import com.stock.corp.entity.CorpDetail;
import com.stock.corp.entity.CorpInfo;
import com.stock.corp.repository.CorpDetailRepository;
import com.stock.corp.repository.CorpInfoRepository;
import com.stock.common.enums.CorpNational;
import com.stock.common.enums.CorpState;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
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

import java.time.LocalDate;
import java.util.Collections;

@Configuration
@AllArgsConstructor
public class CorpDetailBatch {

    private final JobRepository jobRepository;
    private final CorpInfoRepository corpInfoRepository;
    private final CorpDetailRepository corpDetailRepository;
    private final PlatformTransactionManager platformTransactionManager;

    @Bean
    public Job corpDetailCleanupJob() {
        return new JobBuilder("corpDetailCleanupJob", jobRepository)
                .start(corpDetailCleanupStep())
                .build();
    }

    @Bean
    public Step corpDetailCleanupStep() {
        return new StepBuilder("corpDetailCleanupStep", jobRepository)
                .<CorpInfo, CorpDetail>chunk(100, platformTransactionManager)
                .reader(corpInfoReader())
                .processor(corpDetailProcessor())
                .writer(corpDetailWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<CorpInfo> corpInfoReader() {
        return new RepositoryItemReaderBuilder<CorpInfo>()
                .name("corpInfoReader")
                .repository(corpInfoRepository)
                .methodName("findAll")
                .pageSize(100)
                .sorts(Collections.singletonMap("corpCode", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<CorpInfo, CorpDetail> corpDetailProcessor() {
        return item -> {
            //정보가 없다면 신규 생성
            CorpDetail detail = corpDetailRepository.findByCorpCode(item.getCorpCode())
                    .orElse(CorpDetail.builder().corpCode(item.getCorpCode())
                            .state(CorpState.ACTIVE).build());

            // isinCode 앞 2글자로 국가 설정
            if (item.getIsinCode() != null && item.getIsinCode().length() >= 2) {
                String nationCode = item.getIsinCode().substring(0, 2).toUpperCase();
                try {
                    detail.setNational(CorpNational.valueOf(nationCode));
                } catch (IllegalArgumentException e) {
                    // 매핑되는 국가가 없는 경우 기본값 처리 혹은 스킵
                    detail.setNational(null);
                }
            }

            // checkDt가 오늘이 아니면 DEL, 오늘이면 ACTIVE
            if (item.getCheckDt() != null && item.getCheckDt().equals(LocalDate.now())) {
                detail.setState(CorpState.ACTIVE);
            } else {
                detail.setState(CorpState.DEL);
            }

            return detail;
        };
    }

    @Bean
    public RepositoryItemWriter<CorpDetail> corpDetailWriter() {
        return new RepositoryItemWriterBuilder<CorpDetail>()
                .repository(corpDetailRepository)
                .methodName("save")
                .build();
    }
}