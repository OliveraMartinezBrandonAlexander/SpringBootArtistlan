package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetResendRequestDTO {

    @NotBlank(message = "temporaryToken es obligatorio")
    private String temporaryToken;
}
