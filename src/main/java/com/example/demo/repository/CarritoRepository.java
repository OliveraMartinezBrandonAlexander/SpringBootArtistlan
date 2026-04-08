package com.example.demo.repository;

import com.example.demo.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Integer> {

    @Query("""
            SELECT c
            FROM Carrito c
            JOIN FETCH c.obra o
            JOIN FETCH o.usuario
            JOIN FETCH c.solicitud s
            WHERE c.usuario.idUsuario = :idUsuario
            ORDER BY c.fechaAgregado DESC
            """)
    List<Carrito> findByUsuarioId(@Param("idUsuario") Integer idUsuario);

    Optional<Carrito> findByUsuarioIdUsuarioAndObraIdObra(Integer idUsuario, Integer idObra);

    @Query("""
            SELECT c
            FROM Carrito c
            JOIN FETCH c.obra o
            JOIN FETCH o.usuario
            LEFT JOIN FETCH c.solicitud
            WHERE c.usuario.idUsuario = :idUsuario
              AND c.obra.idObra = :idObra
            """)
    Optional<Carrito> findDetalleByUsuarioYObra(@Param("idUsuario") Integer idUsuario,
                                                @Param("idObra") Integer idObra);

    Optional<Carrito> findByObraIdObra(Integer idObra);

    boolean existsByUsuarioIdUsuarioAndObraIdObra(Integer idUsuario, Integer idObra);

    boolean existsByObraIdObra(Integer idObra);

    @Query("""
            SELECT c FROM Carrito c
            WHERE c.reservadaHasta < :ahora
            """)
    List<Carrito> findReservasVencidas(@Param("ahora") LocalDateTime ahora);

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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM Carrito c
            WHERE c.usuario.idUsuario = :idUsuario
            """)
    int eliminarTodosPorUsuario(@Param("idUsuario") Integer idUsuario);
}
