package com.example.demo.service;

import java.util.Optional;

public interface GeminiChatbotFallbackService {

    Optional<String> generateReply(String message);
}
