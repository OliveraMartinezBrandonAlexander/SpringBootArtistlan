package com.example.demo.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioDTO {

    private Integer idServicio;
    private String titulo;
    private String descripcion;
    private String tipoContacto;
    private String contacto;
    private String tecnicas;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Integer likes;
    private Boolean esFavorito;

    private Integer idUsuario;
    private String nombreUsuario;

    private Integer idCategoria;
    private String categoria;
    private String fotoPerfilAutor;
}
