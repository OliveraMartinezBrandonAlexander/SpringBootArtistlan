package com.example.demo.repository;

import com.example.demo.model.Obra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ObraRepository extends JpaRepository<Obra, Integer> {

    @Query("SELECT o FROM Obra o JOIN FETCH o.categoriaObras co JOIN FETCH co.categoria WHERE o.usuario.idUsuario = :idUsuario")
    List<Obra> findByUsuarioIdUsuario(@Param("idUsuario") Integer idUsuario);
    void deleteByUsuarioIdUsuario(Integer idUsuario);
}
