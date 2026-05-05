package com.example.demo.dto.moderacion;

import com.example.demo.enums.EstadoReporte;
import com.example.demo.enums.PrioridadReporte;
import com.example.demo.enums.TipoObjetivoReporte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteResumenDTO {

    private Integer idReporte;
    private TipoObjetivoReporte tipoObjetivo;
    private Integer idObra;
    private Integer idServicio;
    private Integer idUsuarioReportado;
    private String tituloObjetivo;
    private String nombreUsuarioReportante;
    private String nombreUsuarioReportado;
    private String motivo;
    private EstadoReporte estado;
    private PrioridadReporte prioridad;
    private Integer idModeradorAsignado;
    private String nombreModeradorAsignado;
    private LocalDateTime fechaReporte;
}
