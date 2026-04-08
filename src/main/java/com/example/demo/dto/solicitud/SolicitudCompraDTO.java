package com.example.demo.dto.solicitud;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SolicitudCompraDTO {
    Integer idSolicitud;
    Integer idObra;
    String tituloObra;
    Integer idComprador;
    String nombreComprador;
    String fotoComprador;
    Integer idVendedor;
    String nombreVendedor;
    String fotoVendedor;
    String mensajeComprador;
    String estadoSolicitud;
    LocalDateTime fechaCreacion;
    LocalDateTime fechaRespuesta;
    LocalDateTime fechaExpiracionReserva;
    String motivoRechazo;
}
