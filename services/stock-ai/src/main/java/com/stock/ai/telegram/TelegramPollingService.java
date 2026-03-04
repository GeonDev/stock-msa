package com.stock.ai.telegram;

import com.stock.ai.service.AiInsightService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@Slf4j
public class TelegramPollingService {

    private final WebClient.Builder webClientBuilder;
    private final TelegramBotService telegramBotService;
    private final AiInsightService aiInsightService;
    private final String botToken;

    private Long lastUpdateId = 0L;

    public TelegramPollingService(
            WebClient.Builder webClientBuilder,
            TelegramBotService telegramBotService,
            AiInsightService aiInsightService,
            @Value("${telegram.bot.token}") String botToken) {
        this.webClientBuilder = webClientBuilder;
        this.telegramBotService = telegramBotService;
        this.aiInsightService = aiInsightService;
        this.botToken = botToken;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollMessages() {
        if (botToken == null || botToken.isEmpty() || botToken.equals("YOUR_BOT_TOKEN")) {
            return;
        }

        WebClient webClient = webClientBuilder.baseUrl("https://api.telegram.org/bot" + botToken).build();

        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getUpdates")
                        .queryParam("offset", lastUpdateId + 1)
                        .build())
                .retrieve()
                .bodyToMono(TelegramUpdateResponse.class)
                .onErrorResume(e -> {
                    log.error("Error polling Telegram updates: {}", e.getMessage());
                    return null;
                })
                .subscribe(response -> {
                    if (response != null && response.result() != null) {
                        response.result().forEach(this::handleUpdate);
                    }
                });
    }

    private void handleUpdate(TelegramUpdate update) {
        lastUpdateId = Math.max(lastUpdateId, update.update_id());
        if (update.message() != null && update.message().text() != null) {
            String text = update.message().text();
            String chatId = update.message().chat().id().toString();
            log.info("Received message from {}: {}", chatId, text);

            if (text.startsWith("/")) {
                handleCommand(chatId, text);
            } else {
                // If not a command, treat as a question for RAG
                telegramBotService.sendMessage(chatId, "AI is thinking based on financial context...").subscribe();
                String answer = aiInsightService.generateRAGInsight(text);
                telegramBotService.sendMessage(chatId, answer).subscribe();
            }
        }
    }

    private void handleCommand(String chatId, String command) {
        if (command.startsWith("/start")) {
            telegramBotService.sendMessage(chatId, "Welcome to StockMsa AI Bot! Use /report [ticker] for insights or ask me any question about finance.").subscribe();
        } else if (command.startsWith("/report")) {
            String ticker = command.replace("/report", "").trim();
            if (ticker.isEmpty()) {
                telegramBotService.sendMessage(chatId, "Please provide a ticker (e.g., /report 005930)").subscribe();
            } else {
                telegramBotService.sendMessage(chatId, "Analyzing " + ticker + "... (Stay tuned!)").subscribe();
                aiInsightService.getFinancialReport(ticker)
                        .subscribe(report -> telegramBotService.sendMessage(chatId, report).subscribe());
            }
        }
    }

    private record TelegramUpdateResponse(Boolean ok, List<TelegramUpdate> result) {}
    private record TelegramUpdate(Long update_id, TelegramMessage message) {}
    private record TelegramMessage(Long message_id, TelegramChat chat, String text) {}
    private record TelegramChat(Long id, String first_name, String username) {}
}
