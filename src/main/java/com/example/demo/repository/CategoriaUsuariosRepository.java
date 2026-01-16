package com.example.demo.repository;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoriaUsuariosRepository extends JpaRepository<CategoriaUsuarios, CategoriaUsuariosID>
{
    // Devuelve todas las categorias asociadas a un usuario por su id
    List<CategoriaUsuarios> findByUsuario_IdUsuario(Integer idUsuario);

    @Modifying
    @Query("DELETE FROM CategoriaUsuarios cu WHERE cu.usuario.idUsuario = :id")
    void deleteByUsuarioId(@Param("id") Integer id);

}