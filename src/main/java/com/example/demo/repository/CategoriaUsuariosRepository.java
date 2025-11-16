package com.example.demo.repository;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaUsuariosRepository extends JpaRepository<CategoriaUsuarios, CategoriaUsuariosID>
{
    // Devuelve todas las categorias asociadas a un usuario por su id
    List<CategoriaUsuarios> findByUsuario_IdUsuario(Integer idUsuario);

}