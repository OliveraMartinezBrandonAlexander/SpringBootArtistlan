package com.example.demo.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthErrorResponseDTO {
    String message;
    String estadoCuenta;
    String fechaFinSuspension;
}
