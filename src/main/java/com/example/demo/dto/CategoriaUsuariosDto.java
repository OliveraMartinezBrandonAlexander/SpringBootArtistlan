package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CategoriaUsuariosDto {
    private Integer idUsuario;
    private Integer idCategoria;
}
