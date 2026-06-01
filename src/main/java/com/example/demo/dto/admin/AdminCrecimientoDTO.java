package com.example.demo.dto.admin;

import com.example.demo.enums.AdminDashboardTipo;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class AdminCrecimientoDTO {
    AdminDashboardTipo tipo;
    LocalDate fechaReferencia;
    LocalDate fechaInicioSemanaActual;
    LocalDate fechaFinSemanaActual;
    LocalDate fechaInicioSemanaAnterior;
    LocalDate fechaFinSemanaAnterior;
    List<AdminPuntoSerieDTO> serieSemanaActual;
    List<AdminPuntoSerieDTO> serieSemanaAnterior;
    Integer diasComparados;
    long totalSemanaActual;
    long totalSemanaAnterior;
    Double porcentajeCambio;
    boolean periodoAnteriorSinDatos;
    String mensaje;
}
