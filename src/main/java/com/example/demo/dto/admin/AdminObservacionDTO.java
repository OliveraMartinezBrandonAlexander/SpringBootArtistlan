package com.example.demo.dto.admin;

import com.example.demo.enums.AdminTipoDatoEstadistica;
import com.example.demo.enums.AdminTipoEstadistica;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class AdminObservacionDTO {
    Integer idObservacion;
    Integer idAdmin;
    String nombreAdmin;
    AdminTipoEstadistica tipoEstadistica;
    AdminTipoDatoEstadistica tipoDato;
    LocalDate fechaInicioPeriodo;
    LocalDate fechaFinPeriodo;
    String observacion;
    LocalDateTime fechaCreacion;
    LocalDateTime fechaActualizacion;
}
