package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidarPasswordRequestDTO {

    @NotBlank(message = "La contrasena es obligatoria")
    private String contrasena;
}

