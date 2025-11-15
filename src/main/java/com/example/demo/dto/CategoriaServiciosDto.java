package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;
@Builder
@Data
public class CategoriaServiciosDto {
    private Integer idServicio;
    private Integer idCategoria;
}
