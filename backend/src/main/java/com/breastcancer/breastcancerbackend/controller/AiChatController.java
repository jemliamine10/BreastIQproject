package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.ChatRequest;
import com.breastcancer.breastcancerbackend.dto.ChatResponse;
import com.breastcancer.breastcancerbackend.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {
        try {
            ChatResponse response = aiChatService.chat(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            HttpStatus status = determineStatus(ex.getMessage());
            return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
        }
    }

    private HttpStatus determineStatus(String message) {
        if (message == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String lower = message.toLowerCase();
        if (lower.contains("timed out")) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (lower.contains("not running") || lower.contains("unreachable") || lower.contains("unable to reach")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}