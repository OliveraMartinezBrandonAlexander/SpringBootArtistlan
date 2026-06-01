package com.example.demo.dto.admin;

import com.example.demo.enums.AdminTipoDatoEstadistica;
import com.example.demo.enums.AdminTipoEstadistica;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdminObservacionRequestDTO {

    @NotNull
    private AdminTipoEstadistica tipoEstadistica;

    @NotNull
    private AdminTipoDatoEstadistica tipoDato;

    private LocalDate fechaInicioPeriodo;

    private LocalDate fechaFinPeriodo;

    @NotBlank
    private String observacion;
}
