package com.example.demo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarritoDTO {
    private Integer idCarrito;
    private Integer idUsuario;
    private Integer idObra;
    private Integer idArtista;
    private String tituloObra;
    private String descripcion;
    private String estado;
    private String estadoObra;
    private Double precio;
    private String imagen1;
    private String imagen2;
    private String imagen3;
    private String tecnicas;
    private String medidas;
    private Integer likes;
    private String nombreAutor;
    private String nombreCategoria;
    private String fotoPerfilAutor;
    private LocalDateTime fechaAgregado;
}
