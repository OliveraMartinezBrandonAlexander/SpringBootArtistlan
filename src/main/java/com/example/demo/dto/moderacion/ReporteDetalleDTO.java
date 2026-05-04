package com.example.demo.dto.moderacion;

import com.example.demo.enums.AccionResolucionReporte;
import com.example.demo.enums.EstadoCuenta;
import com.example.demo.enums.EstadoModeracion;
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
public class ReporteDetalleDTO {

    private Integer idReporte;
    private TipoObjetivoReporte tipoObjetivo;
    private Integer idObra;
    private Integer idServicio;
    private Integer idUsuarioReportado;
    private String tituloObjetivo;
    private String descripcionObjetivo;
    private String imagenObjetivo;
    private Integer idUsuarioReportante;
    private String nombreUsuarioReportante;
    private Integer idUsuarioDuenoObjetivo;
    private String nombreUsuarioDuenoObjetivo;
    private String motivo;
    private String descripcion;
    private EstadoReporte estado;
    private PrioridadReporte prioridad;
    private Integer idModeradorAsignado;
    private String nombreModeradorAsignado;
    private LocalDateTime fechaReporte;
    private LocalDateTime fechaInicioRevision;
    private LocalDateTime fechaActualizacion;
    private EstadoModeracion estadoModeracionContenido;
    private EstadoCuenta estadoCuentaUsuarioReportado;
    private AccionResolucionReporte accionResolucion;
    private String mensajeRespuesta;
    private LocalDateTime fechaResolucion;
}
