package com.example.demo.dto.moderacion;

import com.example.demo.enums.TipoObjetivoReporte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearReporteRequestDTO {

    private TipoObjetivoReporte tipoObjetivo;
    private Integer idObra;
    private Integer idServicio;
    private Integer idUsuarioReportado;
    private Integer idUsuarioReportante;
    private String motivo;
    private String descripcion;
}
