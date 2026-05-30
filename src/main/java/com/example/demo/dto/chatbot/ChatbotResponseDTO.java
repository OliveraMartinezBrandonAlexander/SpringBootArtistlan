package com.example.demo.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponseDTO {

    private String reply;
    private String intent;
    private String source;
    private Double confidence;
    private List<String> quickReplies;
    private List<ChatbotActionDTO> actions;
}
