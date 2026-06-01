package com.example.demo.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class AdminSerieTemporalDTO {
    LocalDate fechaReferencia;
    LocalDate fechaInicioPeriodo;
    LocalDate fechaFinPeriodo;
    List<AdminPuntoSerieDTO> puntos;
    long totalVentas;
    BigDecimal totalIngresos;
    String mensaje;
}
