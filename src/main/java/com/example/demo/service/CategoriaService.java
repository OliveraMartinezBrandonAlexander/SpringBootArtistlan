package com.example.demo.service;

import com.example.demo.model.Categoria;
import java.util.List;
import java.util.Optional;

public interface CategoriaService {
    Categoria guardar(Categoria c);

    List<Categoria> listar();

    Optional<Categoria> buscarPorId(Integer id);

    Optional<Categoria> actualizar(Integer id, Categoria datos);

    boolean eliminar(Integer id);
}