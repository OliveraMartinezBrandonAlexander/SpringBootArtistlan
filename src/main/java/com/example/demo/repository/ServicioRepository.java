package com.example.demo.repository;

import com.example.demo.enums.EstadoCuenta;
import com.example.demo.enums.EstadoModeracion;
import com.example.demo.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    @Query("""
    SELECT DISTINCT s
    FROM Servicio s
    LEFT JOIN FETCH s.categoriasServicios cs
    LEFT JOIN FETCH cs.categoria
    WHERE s.usuario.idUsuario = :idUsuario
    """)
    List<Servicio> findByUsuarioIdUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("""
    SELECT DISTINCT s
    FROM Servicio s
    LEFT JOIN FETCH s.categoriasServicios cs
    LEFT JOIN FETCH cs.categoria
    WHERE s.usuario.idUsuario = :idUsuario
      AND s.estadoModeracion <> :estadoModeracion
    """)
    List<Servicio> findByUsuarioIdUsuarioAndEstadoModeracionNot(@Param("idUsuario") Integer idUsuario,
                                                                @Param("estadoModeracion") EstadoModeracion estadoModeracion);

    @Query("""
    SELECT DISTINCT s FROM Servicio s
    LEFT JOIN FETCH s.categoriasServicios cs
    LEFT JOIN FETCH cs.categoria
    """)
    List<Servicio> findAllConCategoria();

    @Query("""
    SELECT DISTINCT s FROM Servicio s
    LEFT JOIN FETCH s.categoriasServicios cs
    LEFT JOIN FETCH cs.categoria
    WHERE s.idServicio = :idServicio
    """)
    Optional<Servicio> findByIdConCategoria(@Param("idServicio") Integer idServicio);

    @Query("""
    SELECT DISTINCT s FROM Servicio s
    LEFT JOIN FETCH s.categoriasServicios cs
    LEFT JOIN FETCH cs.categoria
    WHERE s.idServicio = :idServicio
      AND s.usuario.idUsuario = :idUsuario
    """)
    Optional<Servicio> findByIdServicioAndUsuarioIdUsuario(@Param("idServicio") Integer idServicio,
                                                           @Param("idUsuario") Integer idUsuario);

    @Modifying
    @Query("DELETE FROM Servicio s WHERE s.usuario.idUsuario = :id")
    void deleteByUsuarioId(@Param("id") Integer id);

    @Query("""
    SELECT DISTINCT s
    FROM Servicio s
    JOIN FETCH s.usuario u
    LEFT JOIN FETCH s.categoriasServicios cs
    LEFT JOIN FETCH cs.categoria
    WHERE s.oculto = false
      AND s.estadoModeracion NOT IN :estadosModeracion
      AND u.estadoCuenta NOT IN :estadosCuenta
    """)
    List<Servicio> findPublicosVisibles(@Param("estadosModeracion") List<EstadoModeracion> estadosModeracion,
                                        @Param("estadosCuenta") List<EstadoCuenta> estadosCuenta);

    @Query("""
    SELECT DISTINCT s
    FROM Servicio s
    JOIN FETCH s.usuario u
    LEFT JOIN FETCH s.categoriasServicios cs
    LEFT JOIN FETCH cs.categoria
    WHERE s.idServicio = :idServicio
      AND s.oculto = false
      AND s.estadoModeracion NOT IN :estadosModeracion
      AND u.estadoCuenta NOT IN :estadosCuenta
    """)
    Optional<Servicio> findPublicoVisiblePorId(@Param("idServicio") Integer idServicio,
                                               @Param("estadosModeracion") List<EstadoModeracion> estadosModeracion,
                                               @Param("estadosCuenta") List<EstadoCuenta> estadosCuenta);

    List<Servicio> findByUsuario_IdUsuarioAndOcultoFalseAndEstadoModeracionNotIn(Integer idUsuario,
                                                                                  List<EstadoModeracion> estadosModeracion);
}
