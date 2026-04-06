package com.example.demo.repository;

import com.example.demo.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarritoRepository extends JpaRepository<Carrito, Integer> {

    @Query("""
            SELECT c
            FROM Carrito c
            JOIN FETCH c.obra o
            JOIN FETCH o.usuario
            WHERE c.usuario.idUsuario = :idUsuario
            ORDER BY c.fechaAgregado DESC
            """)
    List<Carrito> findByUsuarioId(@Param("idUsuario") Integer idUsuario);

    boolean existsByUsuarioIdUsuarioAndObraIdObra(Integer idUsuario, Integer idObra);

    boolean existsByObraIdObra(Integer idObra);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM Carrito c
            WHERE c.usuario.idUsuario = :idUsuario
              AND c.obra.idObra = :idObra
            """)
    int eliminarPorUsuarioYObra(@Param("idUsuario") Integer idUsuario,
                                @Param("idObra") Integer idObra);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM Carrito c
            WHERE c.obra.idObra = :idObra
            """)
    int eliminarTodosPorObra(@Param("idObra") Integer idObra);
}
