package com.example.demo.service;

import com.example.demo.model.Obra;
import java.util.List;
import java.util.Optional;

public interface ObraService {
    Obra guardar(Obra o);
    List<Obra> listar();
    Optional<Obra> buscarPorId(Integer id);
    Optional<Obra> actualizarObra(Integer id, Obra obra);
    boolean eliminar(Integer id);
}