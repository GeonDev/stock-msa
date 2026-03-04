package com.stock.ai.controller;

import com.stock.ai.service.AiInsightService;
import com.stock.ai.telegram.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/test")
@RequiredArgsConstructor
public class AiTestController {

    private final AiInsightService aiInsightService;
    private final TelegramBotService telegramBotService;

    @GetMapping("/gemini")
    public String testGemini(@RequestParam(defaultValue = "Tell me about the stock market in one sentence.") String prompt) {
        return aiInsightService.generateInsight(prompt);
    }

    @PostMapping("/telegram")
    public String testTelegram(@RequestParam String message) {
        telegramBotService.sendMessage(message).subscribe();
        return "Message sent to Telegram!";
    }

    @GetMapping("/report")
    public String testReport(@RequestParam String stockCode) {
        return aiInsightService.getFinancialReport(stockCode).block();
    }

    @GetMapping("/rag")
    public String testRag(@RequestParam String query) {
        return aiInsightService.generateRAGInsight(query);
    }
}
