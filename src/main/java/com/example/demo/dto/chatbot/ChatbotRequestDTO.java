package com.example.demo.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequestDTO {

    private String message;
    private String sessionId;
    private Integer idUsuario;
}
