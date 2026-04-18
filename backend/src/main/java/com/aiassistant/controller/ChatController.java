package com.aiassistant.controller;

import com.aiassistant.model.ChatRequest;
import com.aiassistant.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        new Thread(() -> chatService.streamResponse(request.message(), emitter)).start();
        return emitter;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
