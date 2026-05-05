package com.example.demo.dto.moderacion;

import com.example.demo.enums.AccionResolucionReporte;
import com.example.demo.enums.TipoRespuestaResolucion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolverReporteRequestDTO {

    private Integer idModerador;
    private AccionResolucionReporte accion;
    private TipoRespuestaResolucion tipoRespuesta;
    private String mensajeRespuesta;
    private String motivoAccion;
    private LocalDateTime fechaFinSuspension;
}
