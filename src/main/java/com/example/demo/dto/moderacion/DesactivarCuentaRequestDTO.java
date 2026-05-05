package com.example.demo.dto.moderacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesactivarCuentaRequestDTO {

    private Integer idUsuarioSolicitante;
    private String contrasenaActual;
    private String motivo;
    private Boolean confirmacion;
}
