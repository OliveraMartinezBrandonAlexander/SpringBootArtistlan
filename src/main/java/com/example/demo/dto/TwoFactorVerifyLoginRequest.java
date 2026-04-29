package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorVerifyLoginRequest {

    @NotBlank(message = "temporaryToken es obligatorio")
    private String temporaryToken;

    @NotBlank(message = "code es obligatorio")
    private String code;
}
