package com.stock.ai.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TelegramBotService {

    private final WebClient webClient;
    private final String defaultChatId;

    public TelegramBotService(
            WebClient.Builder webClientBuilder,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.chat-id}") String defaultChatId) {
        this.webClient = webClientBuilder.baseUrl("https://api.telegram.org/bot" + botToken).build();
        this.defaultChatId = defaultChatId;
    }

    public Mono<String> sendMessage(String text) {
        return sendMessage(defaultChatId, text);
    }

    public Mono<String> sendMessage(String chatId, String text) {
        return webClient.post()
                .uri("/sendMessage")
                .bodyValue(new TelegramSendMessageRequest(chatId, text, "HTML"))
                .retrieve()
                .bodyToMono(String.class);
    }

    private record TelegramSendMessageRequest(String chat_id, String text, String parse_mode) {}
}
