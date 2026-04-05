package com.example.demo.repository;

import com.example.demo.model.Obra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ObraRepository extends JpaRepository<Obra, Integer> {

    @Query("""
            SELECT DISTINCT o
            FROM Obra o
            LEFT JOIN FETCH o.categoriaObras co
            LEFT JOIN FETCH co.categoria
            WHERE o.usuario.idUsuario = :idUsuario
            """)
    List<Obra> findByUsuarioIdUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("""
            SELECT DISTINCT o
            FROM Obra o
            LEFT JOIN FETCH o.categoriaObras co
            LEFT JOIN FETCH co.categoria
            WHERE o.idObra = :idObra
            """)
    Optional<Obra> findByIdConCategoria(@Param("idObra") Integer idObra);

    @Query("""
            SELECT DISTINCT o
            FROM Obra o
            LEFT JOIN FETCH o.categoriaObras co
            LEFT JOIN FETCH co.categoria
            WHERE o.idObra = :idObra
              AND o.usuario.idUsuario = :idUsuario
            """)
    Optional<Obra> findByIdObraAndUsuarioIdUsuario(@Param("idObra") Integer idObra,
                                                   @Param("idUsuario") Integer idUsuario);

    void deleteByUsuarioIdUsuario(Integer idUsuario);

    @Modifying
    @Query("DELETE FROM Obra o WHERE o.usuario.idUsuario = :id")
    void deleteByUsuarioId(@Param("id") Integer id);
}
