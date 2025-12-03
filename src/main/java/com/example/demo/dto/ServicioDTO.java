package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServicioDTO {

    private Integer idServicio;
    private String titulo;
    private String descripcion;
    private String contacto;
    private String tecnicas;

    private Integer idUsuario;
    private String nombreUsuario;

    // Relación con categoría
    private Integer idCategoria;   // id de la tabla categorias
    private String categoria;      // nombre legible de la categoría
}
