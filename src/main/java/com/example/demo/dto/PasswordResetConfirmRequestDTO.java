package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmRequestDTO {

    @NotBlank(message = "temporaryToken es obligatorio")
    private String temporaryToken;

    @NotBlank(message = "code es obligatorio")
    private String code;

    @NotBlank(message = "nuevaContrasena es obligatoria")
    @Size(min = 8, max = 72, message = "nuevaContrasena debe tener entre 8 y 72 caracteres")
    private String nuevaContrasena;

    @NotBlank(message = "confirmarContrasena es obligatoria")
    @Size(min = 8, max = 72, message = "confirmarContrasena debe tener entre 8 y 72 caracteres")
    private String confirmarContrasena;
}
