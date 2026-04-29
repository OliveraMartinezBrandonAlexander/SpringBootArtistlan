package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorResponse {

    private Boolean success;
    private String message;
    private String temporaryToken;
    private String token;
    private UsuarioDTO user;
}
