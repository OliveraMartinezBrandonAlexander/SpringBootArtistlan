package com.example.demo.service;

import com.example.demo.model.CategoriaObras;
import com.example.demo.model.CategoriaObrasID;

import java.util.List;
import java.util.Optional;

public interface CategoriaObrasService {

    CategoriaObras guardar(CategoriaObras co);

    List<CategoriaObras> listar();

    Optional<CategoriaObras> buscarPorId(CategoriaObrasID id);

    void eliminar(CategoriaObrasID id);
}
