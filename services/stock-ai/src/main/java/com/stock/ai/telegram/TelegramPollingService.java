package com.stock.ai.telegram;

import com.stock.ai.service.AiInsightService;
import com.stock.ai.service.UserSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class TelegramPollingService {

    private final WebClient.Builder webClientBuilder;
    private final TelegramBotService telegramBotService;
    private final AiInsightService aiInsightService;
    private final UserSettingService userSettingService;
    private final String botToken;

    private Long lastUpdateId = 0L;

    public TelegramPollingService(
            WebClient.Builder webClientBuilder,
            TelegramBotService telegramBotService,
            AiInsightService aiInsightService,
            UserSettingService userSettingService,
            @Value("${telegram.bot.token}") String botToken) {
        this.webClientBuilder = webClientBuilder;
        this.telegramBotService = telegramBotService;
        this.aiInsightService = aiInsightService;
        this.userSettingService = userSettingService;
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
            telegramBotService.sendMessage(chatId, "Welcome to StockMsa AI Bot!\n\n" +
                    "<b>Commands:</b>\n" +
                    "/report [ticker] - Get AI Insight & Chart\n" +
                    "/watch [ticker] - Add to your watchlist\n" +
                    "/unwatch [ticker] - Remove from watchlist\n" +
                    "/watchlist - View your watchlist\n" +
                    "/alert [ticker] [indicator] [UPPER|LOWER] [value] - Set indicator alert").subscribe();
        } else if (command.startsWith("/report")) {
            String ticker = command.replace("/report", "").trim();
            if (ticker.isEmpty()) {
                telegramBotService.sendMessage(chatId, "Please provide a ticker (e.g., /report 005930)").subscribe();
            } else {
                telegramBotService.sendMessage(chatId, "Analyzing " + ticker + "... (Generating Chart & Insight)").subscribe();
                aiInsightService.getFinancialVisualReport(ticker)
                        .subscribe(report -> telegramBotService.sendPhoto(chatId, report.chartImage(), report.text()).subscribe());
            }
        } else if (command.startsWith("/watch")) {
            String ticker = command.replace("/watch", "").trim();
            if (!ticker.isEmpty()) {
                userSettingService.addToWatchlist(chatId, ticker);
                telegramBotService.sendMessage(chatId, "Added " + ticker + " to your watchlist.").subscribe();
            }
        } else if (command.startsWith("/unwatch")) {
            String ticker = command.replace("/unwatch", "").trim();
            if (!ticker.isEmpty()) {
                userSettingService.removeFromWatchlist(chatId, ticker);
                telegramBotService.sendMessage(chatId, "Removed " + ticker + " from your watchlist.").subscribe();
            }
        } else if (command.startsWith("/watchlist")) {
            List<String> list = userSettingService.getWatchlist(chatId);
            String message = list.isEmpty() ? "Your watchlist is empty." : "<b>Your Watchlist:</b>\n" + String.join(", ", list);
            telegramBotService.sendMessage(chatId, message).subscribe();
        } else if (command.startsWith("/alert")) {
            String[] parts = command.split(" ");
            if (parts.length == 5) {
                try {
                    String ticker = parts[1];
                    String indicator = parts[2];
                    String operator = parts[3];
                    BigDecimal value = new BigDecimal(parts[4]);
                    userSettingService.addAlert(chatId, ticker, indicator, operator, value);
                    telegramBotService.sendMessage(chatId, String.format("Alert set: %s %s %s %s", ticker, indicator, operator, value)).subscribe();
                } catch (Exception e) {
                    telegramBotService.sendMessage(chatId, "Invalid alert format. Use: /alert [ticker] [indicator] [UPPER|LOWER] [value]").subscribe();
                }
            } else {
                telegramBotService.sendMessage(chatId, "Usage: /alert [ticker] [indicator] [UPPER|LOWER] [value]").subscribe();
            }
        }
    }

    private record TelegramUpdateResponse(Boolean ok, List<TelegramUpdate> result) {}
    private record TelegramUpdate(Long update_id, TelegramMessage message) {}
    private record TelegramMessage(Long message_id, TelegramChat chat, String text) {}
    private record TelegramChat(Long id, String first_name, String username) {}
}
