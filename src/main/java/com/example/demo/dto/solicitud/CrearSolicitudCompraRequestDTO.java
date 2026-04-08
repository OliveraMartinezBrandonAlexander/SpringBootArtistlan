package com.example.demo.dto.solicitud;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CrearSolicitudCompraRequestDTO {
    private Integer idObra;
    private Integer idComprador;
    @JsonAlias({"mensaje"})
    private String mensajeComprador;
}
