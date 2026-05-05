package com.example.demo.service;

import com.example.demo.dto.moderacion.ReporteDetalleDTO;
import com.example.demo.dto.moderacion.ReporteResumenDTO;
import com.example.demo.dto.moderacion.ResolverReporteRequestDTO;
import com.example.demo.dto.moderacion.RespuestaModeracionDTO;
import com.example.demo.dto.moderacion.TomarReporteRequestDTO;
import com.example.demo.enums.EstadoReporte;
import com.example.demo.enums.PrioridadReporte;
import com.example.demo.enums.TipoObjetivoReporte;

import java.util.List;

public interface ModeracionService {

    List<ReporteResumenDTO> listarReportes(Integer idModeradorSolicitante,
                                           EstadoReporte estado,
                                           PrioridadReporte prioridad,
                                           TipoObjetivoReporte tipoObjetivo,
                                           Boolean soloMios);

    ReporteDetalleDTO obtenerDetalleReporte(Integer idModeradorSolicitante, Integer idReporte);

    RespuestaModeracionDTO tomarReporte(Integer idReporte, TomarReporteRequestDTO request);

    RespuestaModeracionDTO resolverReporte(Integer idReporte, ResolverReporteRequestDTO request);
}
