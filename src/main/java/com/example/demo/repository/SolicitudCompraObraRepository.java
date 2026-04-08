package com.example.demo.repository;

import com.example.demo.model.SolicitudCompraObra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitudCompraObraRepository extends JpaRepository<SolicitudCompraObra, Integer> {

    @Query("""
            SELECT s FROM SolicitudCompraObra s
            JOIN FETCH s.obra o
            JOIN FETCH s.comprador c
            JOIN FETCH s.vendedor v
            WHERE s.idSolicitud = :id
            """)
    Optional<SolicitudCompraObra> findByIdDetallada(@Param("id") Integer id);

    @Query("""
            SELECT s FROM SolicitudCompraObra s
            JOIN FETCH s.obra o
            JOIN FETCH s.comprador c
            WHERE s.vendedor.idUsuario = :vendedorId
            ORDER BY s.fechaCreacion DESC
            """)
    List<SolicitudCompraObra> findRecibidas(@Param("vendedorId") Integer vendedorId);

    @Query("""
            SELECT s FROM SolicitudCompraObra s
            JOIN FETCH s.obra o
            JOIN FETCH s.vendedor v
            WHERE s.comprador.idUsuario = :compradorId
            ORDER BY s.fechaCreacion DESC
            """)
    List<SolicitudCompraObra> findEnviadas(@Param("compradorId") Integer compradorId);

    boolean existsByObraIdObraAndCompradorIdUsuarioAndEstadoSolicitudIn(Integer idObra, Integer idComprador, List<String> estados);

    long countByObraIdObraAndCompradorIdUsuarioAndEstadoSolicitudIn(Integer idObra, Integer idComprador, List<String> estados);

    boolean existsByObraIdObraAndEstadoSolicitud(Integer idObra, String estadoSolicitud);

    List<SolicitudCompraObra> findByObraIdObraAndEstadoSolicitud(Integer idObra, String estadoSolicitud);

    List<SolicitudCompraObra> findByObraIdObraAndEstadoSolicitudIn(Integer idObra, List<String> estados);

    @Query("""
            SELECT s FROM SolicitudCompraObra s
            WHERE s.estadoSolicitud = 'ACEPTADA'
              AND s.fechaExpiracionReserva IS NOT NULL
              AND s.fechaExpiracionReserva < :ahora
            """)
    List<SolicitudCompraObra> findReservasExpiradas(@Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("""
            UPDATE SolicitudCompraObra s
            SET s.estadoSolicitud = :nuevoEstado,
                s.fechaRespuesta = :fechaRespuesta,
                s.motivoRechazo = :motivo
            WHERE s.obra.idObra = :idObra
              AND s.estadoSolicitud = :estadoActual
              AND s.idSolicitud <> :idSolicitudActual
            """)
    int cerrarPendientesDeObra(@Param("idObra") Integer idObra,
                               @Param("estadoActual") String estadoActual,
                               @Param("nuevoEstado") String nuevoEstado,
                               @Param("fechaRespuesta") LocalDateTime fechaRespuesta,
                               @Param("motivo") String motivo,
                               @Param("idSolicitudActual") Integer idSolicitudActual);

    @Query("""
            SELECT COUNT(s) FROM SolicitudCompraObra s
            WHERE s.estadoSolicitud = 'PENDIENTE'
              AND s.vendedor.idUsuario = :usuarioId
            """)
    long contarPendientesDeUsuario(@Param("usuarioId") Integer usuarioId);

    long deleteByObraIdObra(Integer idObra);
}
