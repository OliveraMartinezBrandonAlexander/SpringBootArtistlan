package com.example.demo.repository;

import com.example.demo.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    @Query("""
    SELECT s FROM Servicio s
    JOIN FETCH s.categoriasServicios cs
    JOIN FETCH cs.categoria c
    WHERE s.usuario.idUsuario = :idUsuario
    """)
    List<Servicio> findByUsuarioIdUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("""
    SELECT s FROM Servicio s
    LEFT JOIN FETCH s.categoriasServicios cs
    LEFT JOIN FETCH cs.categoria
    """)
    List<Servicio> findAllConCategoria();
}
