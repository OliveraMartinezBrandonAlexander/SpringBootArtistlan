package com.example.demo.service;

import com.example.demo.dto.admin.*;
import com.example.demo.enums.AdminDashboardTipo;
import com.example.demo.enums.AdminTipoDatoEstadistica;
import com.example.demo.enums.AdminTipoEstadistica;

import java.time.LocalDate;
import java.util.List;

public interface AdminEstadisticasService {

    List<AdminCategoriaStatsDTO> obtenerCantidadPorCategoria(AdminDashboardTipo tipo);

    AdminSerieTemporalDTO obtenerVentasSemanales(LocalDate fechaReferencia);

    AdminRankingResponseDTO obtenerRanking(AdminDashboardTipo tipo, Integer limit);

    AdminCrecimientoDTO obtenerCrecimientoSemanal(AdminDashboardTipo tipo, LocalDate fechaReferencia);

    List<AdminObservacionDTO> listarObservaciones(
            AdminTipoEstadistica tipoEstadistica,
            AdminTipoDatoEstadistica tipoDato,
            LocalDate fechaInicioPeriodo,
            LocalDate fechaFinPeriodo
    );

    AdminObservacionDTO crearObservacion(AdminObservacionRequestDTO request);

    AdminObservacionDTO actualizarObservacion(Integer idObservacion, AdminObservacionRequestDTO request);

    void eliminarObservacion(Integer idObservacion);
}
