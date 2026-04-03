package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambiarRolRequestDTO {

    @NotBlank(message = "El rol es obligatorio")
    private String rol;
}