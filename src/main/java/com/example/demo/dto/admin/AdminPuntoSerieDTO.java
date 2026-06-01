package com.example.demo.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class AdminPuntoSerieDTO {
    LocalDate fecha;
    String etiqueta;
    long valor;
    BigDecimal monto;
}
