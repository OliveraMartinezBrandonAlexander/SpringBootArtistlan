package com.example.demo.dto.moderacion;

import com.example.demo.enums.PrioridadReporte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TomarReporteRequestDTO {

    private Integer idModerador;
    private PrioridadReporte prioridad;
}
