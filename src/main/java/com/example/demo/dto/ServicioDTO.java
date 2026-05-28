package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaPublicacion;
    private Integer likes;
    private Boolean esFavorito;

    private Integer idUsuario;
    private String nombreUsuario;

    private Integer idCategoria;
    private String categoria;
    private String fotoPerfilAutor;
}
