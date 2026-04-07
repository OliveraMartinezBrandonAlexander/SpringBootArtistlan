package com.example.demo.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ErrorResponseDTO {
    LocalDateTime timestamp;
    int status;
    String error;
    String message;
    String path;
}
