package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObraDTO {
    private Integer idObra;
    private String titulo;
    private String descripcion;
    private String estado;
    private Double precio;
    private String imagen1;
    private String imagen2;
    private String imagen3;
    private String tecnicas;
    private String medidas;
    private Integer likes;

    private Integer idUsuario;
    private Integer idCategoria;

    private String nombreAutor;
    private String nombreCategoria;
    private String fotoPerfilAutor;
}