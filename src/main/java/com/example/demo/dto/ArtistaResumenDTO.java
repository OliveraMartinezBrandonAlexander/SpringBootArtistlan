package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistaResumenDTO {
    private Integer idUsuario;
    private String nombreCompleto;
    private String usuario;
    private String descripcion;
    private String fotoPerfil;
    private Integer idCategoria;
    private String categoria;
    private Integer likes;
    private Boolean esFavorito;
    private String rol;
    private List<String> miniObras;
}
