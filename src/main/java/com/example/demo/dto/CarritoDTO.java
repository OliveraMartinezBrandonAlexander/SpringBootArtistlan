package com.example.demo.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarritoDTO {
    private Integer idCarrito;
    private Integer idUsuario;
    private Integer idObra;
    private Integer idSolicitud;
    private Integer idArtista;
    private String tituloObra;
    private String descripcion;
    private String estadoObra;
    private BigDecimal precio;
    private String imagen1;
    private String tecnicas;
    private String medidas;
    private String nombreAutor;
    private String fotoPerfilAutor;
    private String contactoVendedor;
    private LocalDateTime fechaAgregado;
    private LocalDateTime reservadaHasta;
}
