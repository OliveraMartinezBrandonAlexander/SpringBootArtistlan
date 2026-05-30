package com.example.demo.controller;

import com.example.demo.dto.chatbot.ChatbotRequestDTO;
import com.example.demo.dto.chatbot.ChatbotResponseDTO;
import com.example.demo.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ChatbotResponseDTO> processMessage(@RequestBody ChatbotRequestDTO request) {
        return ResponseEntity.ok(chatbotService.processMessage(request));
    }
}
