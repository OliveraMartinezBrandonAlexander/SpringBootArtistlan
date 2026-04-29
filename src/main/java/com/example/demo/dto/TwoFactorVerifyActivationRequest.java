package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorVerifyActivationRequest {

    @NotBlank(message = "code es obligatorio")
    private String code;
}
