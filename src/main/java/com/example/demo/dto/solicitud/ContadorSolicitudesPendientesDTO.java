package com.example.demo.dto.solicitud;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContadorSolicitudesPendientesDTO {
    long pendientes;
}
