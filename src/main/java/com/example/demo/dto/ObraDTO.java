package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaPublicacion;

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
