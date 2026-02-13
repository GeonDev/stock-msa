package com.stock.corp.batchJob;

import com.stock.common.consts.ApplicationConstants;
import com.stock.common.enums.SectorType;
import com.stock.corp.client.DartClient;
import com.stock.corp.client.DartCompanyResponse;
import com.stock.corp.entity.CorpDetail;
import com.stock.corp.entity.CorpInfo;
import com.stock.corp.repository.CorpDetailRepository;
import com.stock.corp.repository.CorpInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SectorUpdateBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CorpInfoRepository corpInfoRepository;
    private final CorpDetailRepository corpDetailRepository;
    private final DartClient dartClient;

    @Bean
    public Job sectorUpdateJob() {
        return new JobBuilder("sectorUpdateJob", jobRepository)
                .start(sectorUpdateStep())
                .build();
    }

    @Bean
    @JobScope
    public Step sectorUpdateStep() {
        return new StepBuilder("sectorUpdateStep", jobRepository)
                .<CorpInfo, CorpDetail>chunk(ApplicationConstants.STOCK_CORP_CHUNK_SIZE, transactionManager)
                .reader(corpInfoReader())
                .processor(sectorUpdateProcessor())
                .writer(sectorUpdateWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<CorpInfo> corpInfoReader() {
        return new RepositoryItemReaderBuilder<CorpInfo>()
                .name("corpInfoReader")
                .repository(corpInfoRepository)
                .methodName("findAll")
                .sorts(Collections.singletonMap("corpCode", Sort.Direction.ASC))
                .pageSize(ApplicationConstants.STOCK_CORP_CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemProcessor<CorpInfo, CorpDetail> sectorUpdateProcessor() {
        return corpInfo -> {
            // DART 법인코드(8자리)로 업종 정보 조회
            DartCompanyResponse response = dartClient.getCompanyInfo(corpInfo.getCorpCode());
            
            if (response == null || !"000".equals(response.getStatus())) {
                log.warn("업종 정보 조회 실패: {} ({})", corpInfo.getCorpName(), corpInfo.getCorpCode());
                return null;
            }

            SectorType sector = SectorType.fromCode(response.getInduty_code());
            
            CorpDetail detail = corpDetailRepository.findById(corpInfo.getCorpCode())
                    .orElseGet(() -> CorpDetail.builder().corpCode(corpInfo.getCorpCode()).build());
            
            detail.setSector(sector);
            return detail;
        };
    }

    @Bean
    public RepositoryItemWriter<CorpDetail> sectorUpdateWriter() {
        return new RepositoryItemWriterBuilder<CorpDetail>()
                .repository(corpDetailRepository)
                .methodName("save")
                .build();
    }
}
