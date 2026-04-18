package com.aiassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final WebClient webClient;
    private final PdfIngestionService pdfIngestionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.api.model}")
    private String model;

    public ChatService(
            @Value("${ollama.api.url}") String ollamaUrl,
            PdfIngestionService pdfIngestionService) {
        this.pdfIngestionService = pdfIngestionService;
        this.webClient = WebClient.builder()
                .baseUrl(ollamaUrl)
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public void streamResponse(String userMessage, SseEmitter emitter) {
        String context = pdfIngestionService.getRelevantContext(userMessage, 3);

        String systemPrompt = context.isEmpty()
                ? "You are a helpful AI assistant. No PDF documents have been uploaded yet. Let the user know they can upload PDFs using the upload button."
                : "You are a helpful AI assistant. Answer questions using ONLY the context below from uploaded PDF documents. If the answer is not in the context, say so.\n\nContext:\n" + context;

        // Ollama /api/chat request body
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        // Ollama streams newline-delimited JSON (not SSE)
        webClient.post()
                .uri("/api/chat")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        line -> handleOllamaLine(line, emitter),
                        error -> {
                            try {
                                emitter.send(SseEmitter.event().data("{\"error\":true}", MediaType.TEXT_PLAIN));
                                emitter.complete();
                            } catch (Exception ignored) {}
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().data("{\"done\":true}", MediaType.TEXT_PLAIN));
                                emitter.complete();
                            } catch (Exception ignored) {}
                        }
                );
    }

    private void handleOllamaLine(String line, SseEmitter emitter) {
        if (line == null || line.isBlank()) return;
        try {
            JsonNode node = objectMapper.readTree(line);

            // Extract token from message.content
            String token = node.path("message").path("content").asText("");
            if (!token.isEmpty()) {
                String payload = objectMapper.writeValueAsString(Map.of("t", token));
                emitter.send(SseEmitter.event().data(payload, MediaType.TEXT_PLAIN));
            }

            // done:true means the stream has finished
            if (node.path("done").asBoolean(false)) {
                emitter.send(SseEmitter.event().data("{\"done\":true}", MediaType.TEXT_PLAIN));
                emitter.complete();
            }
        } catch (Exception ignored) {}
    }
}
