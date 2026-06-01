package com.example.demo.dto.meta;

import com.example.demo.enums.TipoMetaPersonal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MetaPersonalUpdateDTO {

    @NotNull
    private TipoMetaPersonal tipoMeta;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    private BigDecimal objetivo;

    @NotNull
    private LocalDate fechaInicio;

    @NotNull
    private LocalDate fechaFin;
}
