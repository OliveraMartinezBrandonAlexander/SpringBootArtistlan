package com.example.demo.service;

import com.example.demo.dto.moderacion.CrearReporteRequestDTO;
import com.example.demo.dto.moderacion.ReporteDetalleDTO;
import com.example.demo.dto.moderacion.ReporteResumenDTO;

import java.util.List;

public interface ReporteService {

    ReporteDetalleDTO crearReporte(CrearReporteRequestDTO request);

    List<ReporteResumenDTO> listarReportesDeUsuario(Integer idUsuario);

    ReporteDetalleDTO obtenerDetalleReporteDeUsuario(Integer idUsuario, Integer idReporte);
}
