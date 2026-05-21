package com.example.demo.repository;

import com.example.demo.enums.EstadoCuenta;
import com.example.demo.enums.EstadoModeracion;
import com.example.demo.model.Obra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            JOIN FETCH o.usuario u
            LEFT JOIN FETCH o.categoriaObras co
            LEFT JOIN FETCH co.categoria
            WHERE u.idUsuario = :idUsuario
            """)
    List<Obra> findByUsuarioIdUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("""
            SELECT DISTINCT o
            FROM Obra o
            JOIN FETCH o.usuario u
            LEFT JOIN FETCH o.categoriaObras co
            LEFT JOIN FETCH co.categoria
            WHERE u.idUsuario = :idUsuario
              AND o.estadoModeracion <> :estadoModeracion
            """)
    List<Obra> findByUsuarioIdUsuarioAndEstadoModeracionNot(@Param("idUsuario") Integer idUsuario,
                                                            @Param("estadoModeracion") EstadoModeracion estadoModeracion);

    @Query("""
            SELECT DISTINCT o
            FROM Obra o
            JOIN FETCH o.usuario u
            LEFT JOIN FETCH o.categoriaObras co
            LEFT JOIN FETCH co.categoria
            WHERE u.idUsuario = :idUsuario
              AND COALESCE(o.oculta, false) = false
              AND o.estadoModeracion NOT IN :estadosModeracion
            """)
    List<Obra> findPropiasActivasByUsuarioId(@Param("idUsuario") Integer idUsuario,
                                             @Param("estadosModeracion") List<EstadoModeracion> estadosModeracion);

    @Query("""
            SELECT DISTINCT o
            FROM Obra o
            LEFT JOIN FETCH o.usuario
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

    @Query("""
            SELECT DISTINCT o
            FROM Obra o
            JOIN FETCH o.usuario u
            LEFT JOIN FETCH o.categoriaObras co
            LEFT JOIN FETCH co.categoria
            WHERE o.oculta = false
              AND o.estadoModeracion NOT IN :estadosModeracion
              AND u.estadoCuenta NOT IN :estadosCuenta
            """)
    List<Obra> findPublicasVisibles(@Param("estadosModeracion") List<EstadoModeracion> estadosModeracion,
                                    @Param("estadosCuenta") List<EstadoCuenta> estadosCuenta);

    @Query(
            value = """
            SELECT DISTINCT o
            FROM Obra o
            JOIN o.usuario u
            LEFT JOIN o.categoriaObras co
            LEFT JOIN co.categoria c
            WHERE o.oculta = false
              AND o.estadoModeracion NOT IN :estadosModeracion
              AND u.estadoCuenta NOT IN :estadosCuenta
              AND (
                    :q IS NULL
                    OR :q = ''
                    OR LOWER(o.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(o.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              AND (:idCategoria IS NULL OR c.idCategoria = :idCategoria)
              AND (
                    :categoria IS NULL
                    OR :categoria = ''
                    OR LOWER(c.nombreCategoria) = LOWER(:categoria)
                  )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT o.idObra)
            FROM Obra o
            JOIN o.usuario u
            LEFT JOIN o.categoriaObras co
            LEFT JOIN co.categoria c
            WHERE o.oculta = false
              AND o.estadoModeracion NOT IN :estadosModeracion
              AND u.estadoCuenta NOT IN :estadosCuenta
              AND (
                    :q IS NULL
                    OR :q = ''
                    OR LOWER(o.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(o.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              AND (:idCategoria IS NULL OR c.idCategoria = :idCategoria)
              AND (
                    :categoria IS NULL
                    OR :categoria = ''
                    OR LOWER(c.nombreCategoria) = LOWER(:categoria)
                  )
            """
    )
    Page<Obra> findPublicasVisiblesPaginado(
            @Param("estadosModeracion") List<EstadoModeracion> estadosModeracion,
            @Param("estadosCuenta") List<EstadoCuenta> estadosCuenta,
            @Param("q") String q,
            @Param("categoria") String categoria,
            @Param("idCategoria") Integer idCategoria,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT o
            FROM Obra o
            JOIN FETCH o.usuario u
            LEFT JOIN FETCH o.categoriaObras co
            LEFT JOIN FETCH co.categoria
            WHERE o.idObra = :idObra
              AND o.oculta = false
              AND o.estadoModeracion NOT IN :estadosModeracion
              AND u.estadoCuenta NOT IN :estadosCuenta
            """)
    Optional<Obra> findPublicaVisiblePorId(@Param("idObra") Integer idObra,
                                           @Param("estadosModeracion") List<EstadoModeracion> estadosModeracion,
                                           @Param("estadosCuenta") List<EstadoCuenta> estadosCuenta);

    List<Obra> findByUsuario_IdUsuarioAndOcultaFalseAndEstadoModeracionNotIn(Integer idUsuario,
                                                                              List<EstadoModeracion> estadosModeracion);

    @Query("""
            SELECT o
            FROM Obra o
            JOIN FETCH o.usuario u
            WHERE u.idUsuario IN :usuarioIds
              AND o.oculta = false
              AND o.estadoModeracion NOT IN :estadosModeracion
              AND u.estadoCuenta NOT IN :estadosCuenta
            ORDER BY u.idUsuario ASC, o.fechaPublicacion DESC, o.idObra DESC
            """)
    List<Obra> findPublicasVisiblesPorUsuarioIds(
            @Param("usuarioIds") List<Integer> usuarioIds,
            @Param("estadosModeracion") List<EstadoModeracion> estadosModeracion,
            @Param("estadosCuenta") List<EstadoCuenta> estadosCuenta
    );
}
