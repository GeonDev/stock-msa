package com.stock.ai.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
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
        String targetChatId = (chatId == null) ? defaultChatId : chatId;
        return webClient.post()
                .uri("/sendMessage")
                .bodyValue(new TelegramSendMessageRequest(targetChatId, text, "HTML"))
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> sendPhoto(String chatId, byte[] photo, String caption) {
        String targetChatId = (chatId == null) ? defaultChatId : chatId;
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("chat_id", targetChatId);
        builder.part("photo", new ByteArrayResource(photo))
                .header("Content-Disposition", "form-data; name=\"photo\"; filename=\"chart.png\"");
        builder.part("caption", caption);
        builder.part("parse_mode", "HTML");

        return webClient.post()
                .uri("/sendPhoto")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(String.class);
    }

    private record TelegramSendMessageRequest(String chat_id, String text, String parse_mode) {}
}
