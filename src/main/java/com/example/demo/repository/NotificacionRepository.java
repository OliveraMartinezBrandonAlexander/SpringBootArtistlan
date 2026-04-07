package com.example.demo.repository;

import com.example.demo.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    @Query("""
            SELECT n FROM Notificacion n
            LEFT JOIN FETCH n.usuarioOrigen
            WHERE n.usuarioDestino.idUsuario = :usuarioId
              AND n.eliminada = false
            ORDER BY n.fechaCreacion DESC
            """)
    List<Notificacion> findActivasPorUsuario(@Param("usuarioId") Integer usuarioId);

    @Query("""
            SELECT n FROM Notificacion n
            LEFT JOIN FETCH n.usuarioOrigen
            WHERE n.idNotificacion = :id
              AND n.usuarioDestino.idUsuario = :usuarioId
              AND n.eliminada = false
            """)
    Optional<Notificacion> findDetalle(@Param("id") Integer id, @Param("usuarioId") Integer usuarioId);

    long countByUsuarioDestinoIdUsuarioAndLeidaFalseAndEliminadaFalse(Integer usuarioId);

    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.leida = true
            WHERE n.usuarioDestino.idUsuario = :usuarioId
              AND n.eliminada = false
            """)
    int marcarTodasLeidas(@Param("usuarioId") Integer usuarioId);
}
