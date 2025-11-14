package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
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
    private int likes;
    private Integer idUsuario;
}
