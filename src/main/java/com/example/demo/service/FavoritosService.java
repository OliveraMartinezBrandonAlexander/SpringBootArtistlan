package com.example.demo.service;

import com.example.demo.model.Favoritos;

import java.util.List;
import java.util.Optional;

public interface FavoritosService {

    Favoritos guardar(Favoritos f);

    List<Favoritos> listar();

    Optional<Favoritos> buscarPorId(Integer id);

    void eliminar(Integer id);
}
