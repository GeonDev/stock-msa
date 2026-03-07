package com.stock.ai.controller;

import com.stock.ai.service.AiInsightService;
import com.stock.ai.telegram.TelegramBotService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatClient chatClient;
    private final AiInsightService aiInsightService;
    private final TelegramBotService telegramBotService;

    public AiController(ChatClient.Builder builder, AiInsightService aiInsightService, TelegramBotService telegramBotService) {
        this.chatClient = builder.build();
        this.aiInsightService = aiInsightService;
        this.telegramBotService = telegramBotService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "안녕! 넌 누구니?") String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/gemini")
    public String geminiInsight(@RequestParam(defaultValue = "Tell me about the stock market in one sentence.") String prompt) {
        return aiInsightService.generateInsight(prompt);
    }

    @PostMapping("/telegram")
    public String sendTelegramMsg(@RequestParam String message) {
        telegramBotService.sendMessage(message).subscribe();
        return "Message sent to Telegram!";
    }

    @GetMapping("/report")
    public String financialReport(@RequestParam String stockCode) {
        return aiInsightService.getFinancialReport(stockCode).block();
    }

    @GetMapping("/rag")
    public String ragInsight(@RequestParam String query) {
        return aiInsightService.generateRAGInsight(query);
    }
}
