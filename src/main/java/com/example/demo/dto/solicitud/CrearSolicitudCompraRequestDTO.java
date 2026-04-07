package com.example.demo.dto.solicitud;

import lombok.Data;

@Data
public class CrearSolicitudCompraRequestDTO {
    private Integer idObra;
    private Integer idComprador;
    private String mensajeComprador;
}
