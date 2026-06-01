package com.example.demo.dto.meta;

import com.example.demo.enums.EstadoMetaPersonal;
import com.example.demo.enums.TipoMetaPersonal;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class MetaPersonalProgresoDTO {
    Integer idMeta;
    TipoMetaPersonal tipoMeta;
    BigDecimal objetivo;
    EstadoMetaPersonal estado;
    LocalDate fechaInicio;
    LocalDate fechaFin;
    BigDecimal progresoActual;
    BigDecimal porcentaje;
    BigDecimal porcentajeVisual;
    String progresoTexto;
    String mensajeEstado;
    Boolean editable;
    Boolean cancelable;
}
