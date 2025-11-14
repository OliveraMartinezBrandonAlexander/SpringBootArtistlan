package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FavoritosDTO
{
    private Integer id_favorito;
    private Integer id_usuario;
    private Integer id_obra;
    private Integer id_servicio;

}
