package com.example.demo.service;

import com.example.demo.model.Favoritos;
import com.example.demo.dto.FavoritosDTO;

import java.util.List;

public interface FavoritosService {

    Favoritos agregarFavorito(FavoritosDTO dto);

    void eliminarFavorito(FavoritosDTO dto);

    List<Favoritos> obtenerFavoritosPorUsuario(Long idUsuario);

    int likesPorObra(Long idObra);

    int likesPorServicio(Long idServicio);

    int likesPorArtista(Long idArtista);

    boolean esObraFavorita(Integer idUsuario, Integer idObra);

    boolean esServicioFavorito(Integer idUsuario, Integer idServicio);

    boolean esArtistaFavorito(Integer idUsuario, Integer idArtista);
}
