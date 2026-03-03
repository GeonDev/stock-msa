package com.stock.corp.batchJob;

import com.stock.corp.batchJob.ItemReader.CorpInfoItemReader;
import com.stock.corp.entity.CorpInfo;
import com.stock.corp.repository.CorpInfoRepository;
import com.stock.corp.service.CorpInfoService;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import static com.stock.common.consts.ApplicationConstants.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CorpInfoBatch {

    private final JobRepository jobRepository;
    private final CorpInfoRepository corpInfoRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final CorpInfoService corpInfoService;
    private final RestClient restClient;
    private final CacheManager cacheManager;

    @Value("${dart.api-key}")
    private String dartApiKey;


    @Bean
    public Job corpDataJob() {
        return new JobBuilder("corpDataJob", jobRepository)
                .start(corpDataStep())
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            cacheManager.getCache("corpCache").clear();
                            log.info("corpCache evicted after corpDataJob completion");
                        }
                    }
                })
                .build();
    }

    // DART Corp Code 캐시 (배치 실행 중 공유)
    private final Map<String, String> dartCorpCodeCache = new HashMap<>();


    @Bean
    @StepScope
    public CorpInfoItemReader corpInfoItemReader() {
        return new CorpInfoItemReader(corpInfoService);
    }

    @Bean
    public Step corpDataStep() {
        return new StepBuilder("corpDataStep", jobRepository)
                .<CorpInfo, CorpInfo>chunk(STOCK_CORP_CHUNK_SIZE, platformTransactionManager)
                .reader(corpInfoItemReader())
                .processor(corpDataProcessor())
                .writer(corpItemWriter())
                .build();
    }


    @Bean
    public ItemProcessor<CorpInfo, CorpInfo> corpDataProcessor() {
        return item -> {

            // 캐시가 비어있으면 DART Corp Code 다운로드
            if (dartCorpCodeCache.isEmpty()) {
                downloadDartCorpCodes();
            }

            // Stock code에서 A 제거하여 DART Corp Code 조회
            String cleanStockCode = item.getStockCode().replace("A", "");
            String dartCorpCode = dartCorpCodeCache.get(cleanStockCode);
            
            if (dartCorpCode != null) {
                item.setDartCorpCode(dartCorpCode);
            }

            return item;
        };
    }

    /**
     * DART Corp Code XML 다운로드 및 파싱
     */
    private void downloadDartCorpCodes() {
        try {
            log.info("Downloading DART corp code list...");

            // 네트워크 스트림을 직접 ZipInputStream으로 연결하여 메모리 점유 최소화
            restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host(DART_URL)
                            .path(DART_CORP_CODE_URL)
                            .queryParam("crtfc_key", dartApiKey)
                            .build())
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            try (InputStream is = response.getBody();
                                 ZipInputStream zis = new ZipInputStream(is)) {
                                if (zis.getNextEntry() != null) {
                                    parseCorpCodeXml(zis);
                                }
                            }
                        } else {
                            log.error("Failed to download DART corp code list. Status: {}", response.getStatusCode());
                        }
                        return null; // Body 추출 없이 스트림 처리 종료
                    });

            log.info("Loaded {} DART corp codes", dartCorpCodeCache.size());

        } catch (Exception e) {
            log.error("Failed to download DART corp codes", e);
        }
    }

    /**
     * XML 스트리밍 파싱하여 캐시 구축 (StAX 사용)
     */
    private void parseCorpCodeXml(InputStream is) {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            // 보안 설정: DTD 및 외부 엔티티 차단
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

            XMLStreamReader reader = factory.createXMLStreamReader(is);
            
            String currentCorpCode = null;
            String currentStockCode = null;
            String tagContent = null;

            while (reader.hasNext()) {
                int event = reader.next();
                
                switch (event) {
                    case XMLStreamConstants.CHARACTERS:
                        tagContent = reader.getText().trim();
                        break;
                    
                    case XMLStreamConstants.END_ELEMENT:
                        String tagName = reader.getLocalName();
                        if ("corp_code".equals(tagName)) {
                            currentCorpCode = tagContent;
                        } else if ("stock_code".equals(tagName)) {
                            currentStockCode = tagContent;
                        } else if ("list".equals(tagName)) {
                            if (currentCorpCode != null && currentStockCode != null && !currentStockCode.isEmpty()) {
                                dartCorpCodeCache.put(currentStockCode, currentCorpCode);
                            }
                            currentCorpCode = null;
                            currentStockCode = null;
                        }
                        break;
                }
            }
            reader.close();
        } catch (Exception e) {
            log.error("Error parsing DART corp code XML", e);
        }
    }


    @Bean
    public RepositoryItemWriter<CorpInfo> corpItemWriter() {
        return new RepositoryItemWriterBuilder<CorpInfo>()
                .repository(corpInfoRepository)
                .methodName("save")
                .build();
    }

}
