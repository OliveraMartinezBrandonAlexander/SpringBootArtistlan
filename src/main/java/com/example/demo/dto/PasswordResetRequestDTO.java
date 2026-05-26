package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequestDTO {

    @NotBlank(message = "usuarioOCorreo es obligatorio")
    private String usuarioOCorreo;
}
