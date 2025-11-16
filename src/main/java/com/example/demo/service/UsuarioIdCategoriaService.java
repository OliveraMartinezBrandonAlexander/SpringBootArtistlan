package com.example.demo.service;

import com.example.demo.dto.UsuarioIdCategoriaDTO;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;

import java.util.List;
import java.util.Optional;

public interface UsuarioIdCategoriaService {

    List<UsuarioIdCategoriaDTO> obtenerTodasCategoriasPorUsuario(Integer usuarioId);

    CategoriaUsuarios guardar(CategoriaUsuarios cu);

    List<CategoriaUsuarios> listar();

    Optional<CategoriaUsuarios> buscarPorId(CategoriaUsuariosID id);

    void eliminar(CategoriaUsuariosID id);


    Optional<CategoriaUsuarios> actualizar(CategoriaUsuariosID id, CategoriaUsuarios nuevosDatos);
}
