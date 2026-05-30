package com.example.demo.service;

import com.example.demo.dto.chatbot.ChatbotRequestDTO;
import com.example.demo.dto.chatbot.ChatbotResponseDTO;

public interface ChatbotService {

    ChatbotResponseDTO processMessage(ChatbotRequestDTO request);
}
