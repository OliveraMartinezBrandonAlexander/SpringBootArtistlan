package com.example.demo.service;

import com.example.demo.dto.ObraDTO;
import com.example.demo.model.Obra;
import java.util.List;
import java.util.Optional;

public interface ObraService {
    Obra guardar(Obra o);
    List<Obra> listar();
    Optional<Obra> buscarPorId(Integer id);
    Optional<Obra> actualizarObra(Integer id, Obra obra);
    boolean eliminar(Integer id);
    Optional<Obra> actualizarImagen1(Integer id, String urlImagen);
    Obra guardarObraConCategoria(Integer usuarioId, ObraDTO obraDTO);
    List<Obra> buscarPorUsuarioId(Integer usuarioId);
    void eliminarPorUsuarioId(Integer usuarioId);
}