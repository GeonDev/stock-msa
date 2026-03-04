package com.stock.ai.service;

import com.stock.ai.client.DartFinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosurePollingService {

    private final DartFinanceClient dartFinanceClient;
    private final Set<String> processedReceipts = new HashSet<>();

    @Scheduled(fixedDelay = 900000) // 15 minutes
    public void pollDisclosures() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info("Polling DART disclosures for {}", today);

        // pblntfTy=A: 정기공시
        dartFinanceClient.getDisclosureList(today, today, "A")
                .subscribe(response -> {
                    if (response != null && response.list() != null) {
                        response.list().stream()
                                .filter(d -> isFinancialReport(d.report_nm()))
                                .filter(d -> !processedReceipts.contains(d.rcept_no()))
                                .forEach(this::processDisclosure);
                    }
                });
    }

    private boolean isFinancialReport(String reportName) {
        return reportName.contains("사업보고서") || 
               reportName.contains("분기보고서") || 
               reportName.contains("반기보고서");
    }

    private void processDisclosure(DartFinanceClient.DartDisclosure disclosure) {
        log.info("New financial report detected: {} - {}", disclosure.corp_name(), disclosure.report_nm());
        
        String reportCode = extractReportCode(disclosure.report_nm());
        if (reportCode != null) {
            dartFinanceClient.triggerFinanceBatch(disclosure.corp_code(), reportCode)
                    .subscribe(status -> {
                        log.info("Triggered finance batch for {}: {}", disclosure.corp_name(), status);
                        processedReceipts.add(disclosure.rcept_no());
                    });
        }
    }

    private String extractReportCode(String reportName) {
        if (reportName.contains("1분기보고서")) return "Q1";
        if (reportName.contains("반기보고서")) return "SEMI";
        if (reportName.contains("3분기보고서")) return "Q3";
        if (reportName.contains("사업보고서")) return "ANNUAL";
        return null;
    }
}
