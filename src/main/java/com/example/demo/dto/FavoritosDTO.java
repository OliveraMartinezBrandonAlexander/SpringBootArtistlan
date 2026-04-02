package com.example.demo.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoritosDTO {
    private Integer idFavorito;
    private Integer idUsuario;
    private Integer idObra;
    private Integer idServicio;
    private Integer idArtista;
}
