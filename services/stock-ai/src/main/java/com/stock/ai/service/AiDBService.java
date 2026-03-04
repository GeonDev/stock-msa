package com.stock.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDBService {

    private final VectorStore vectorStore;

    public void storeFinancialSummary(String stockCode, String summary) {
        Document doc = new Document(summary, Map.of("stockCode", stockCode, "type", "financial_summary"));
        vectorStore.add(List.of(doc));
        log.info("Stored financial summary for {} in vector store", stockCode);
    }

    public List<Document> searchRelatedFinances(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(3)
        );
    }
}
