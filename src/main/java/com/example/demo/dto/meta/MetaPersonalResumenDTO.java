package com.example.demo.dto.meta;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class MetaPersonalResumenDTO {
    Integer total;
    Integer activas;
    Integer porComenzar;
    Integer enProceso;
    Integer completadas;
    Integer expiradas;
    Integer canceladas;
    BigDecimal porcentajeGlobal;
}
