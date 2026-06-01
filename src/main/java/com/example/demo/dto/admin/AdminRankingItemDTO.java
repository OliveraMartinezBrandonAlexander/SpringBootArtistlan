package com.example.demo.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminRankingItemDTO {
    Integer id;
    String nombre;
    long total;
    String descripcionSecundaria;
    String imagen;
    String imagenAutor;
    String autor;
    String subtitulo;
    String contacto;
    String tipoContacto;
}
