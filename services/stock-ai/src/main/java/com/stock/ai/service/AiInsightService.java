package com.stock.ai.service;

import com.stock.ai.client.CorpClient;
import com.stock.ai.client.FinanceClient;
import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.common.dto.CorpInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiInsightService {

    private final ChatClient chatClient;
    private final CorpClient corpClient;
    private final FinanceClient financeClient;
    private final PromptService promptService;
    private final AiDBService aiDBService;
    private final VisualizationService visualizationService;

    public AiInsightService(ChatClient.Builder builder, CorpClient corpClient, FinanceClient financeClient, 
                             PromptService promptService, AiDBService aiDBService, 
                             VisualizationService visualizationService) {
        this.chatClient = builder.build();
        this.corpClient = corpClient;
        this.financeClient = financeClient;
        this.promptService = promptService;
        this.aiDBService = aiDBService;
        this.visualizationService = visualizationService;
    }

    public record VisualReport(String text, byte[] chartImage) {}

    public Mono<VisualReport> getFinancialVisualReport(String stockCode) {
        return Mono.zip(
                corpClient.getCorpInfo(stockCode),
                financeClient.getLatestIndicator(stockCode)
        ).map(tuple -> {
            CorpInfoDto corpInfo = tuple.getT1();
            CorpFinanceIndicatorDto indicators = tuple.getT2();
            
            String summary = promptService.createFinancialSummary(corpInfo, indicators);
            
            // Store in vector DB for RAG
            aiDBService.storeFinancialSummary(stockCode, summary);
            
            String prompt = promptService.createScoringPrompt(summary);
            String aiOpinion = generateInsight(prompt);

            byte[] chart = visualizationService.createIndicatorChart(corpInfo.getCorpName(), indicators);
            
            return new VisualReport(aiOpinion, chart);
        });
    }

    public String generateInsight(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    public String generateRAGInsight(String question) {
        List<Document> relatedDocs = aiDBService.searchRelatedFinances(question);
        String context = relatedDocs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n---\n"));

        String prompt = "Answer the following question based on the provided financial context.\n\n" +
                        "Context:\n" + context + "\n\n" +
                        "Question: " + question;
        
        return generateInsight(prompt);
    }

    public Mono<String> getFinancialReport(String stockCode) {
        return Mono.zip(
                corpClient.getCorpInfo(stockCode),
                financeClient.getLatestIndicator(stockCode)
        ).map(tuple -> {
            CorpInfoDto corpInfo = tuple.getT1();
            CorpFinanceIndicatorDto indicators = tuple.getT2();
            
            String summary = promptService.createFinancialSummary(corpInfo, indicators);
            
            // Store in vector DB for RAG
            aiDBService.storeFinancialSummary(stockCode, summary);
            
            String prompt = promptService.createScoringPrompt(summary);
            return generateInsight(prompt);
        });
    }
}
