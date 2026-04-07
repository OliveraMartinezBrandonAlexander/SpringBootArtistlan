package com.example.demo.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObraDTO {
    private Integer idObra;
    private String titulo;
    private String descripcion;
    private String estado;
    private BigDecimal precio;
    private String imagen1;
    private String imagen2;
    private String imagen3;
    private String tecnicas;
    private String medidas;
    private Boolean confirmacionAutoria;

    private Integer likes;
    private Boolean esFavorito;
    private Integer idUsuario;
    private Integer idCategoria;
    private String nombreAutor;
    private String nombreCategoria;
    private String fotoPerfilAutor;

    private Boolean editable;
    private Boolean eliminable;
    private Boolean puedeSolicitarCompra;
}
