package com.example.demo.repository;

import com.example.demo.enums.EstadoCuenta;
import com.example.demo.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @Query("SELECT u FROM Usuario u WHERE UPPER(u.rol) = 'ADMIN'")
    List<Usuario> findAdmins();

    boolean existsByUsuario(String usuario);
    boolean existsByCorreo(String correo);

    Optional<Usuario> findFirstByUsuarioIgnoreCaseOrCorreoIgnoreCase(String usuario, String correo);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.categoriasUsuarios cu LEFT JOIN FETCH cu.categoria WHERE u.idUsuario = :id")
    Optional<Usuario> findByIdConCategorias(@Param("id") Integer id);

    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.categoriasUsuarios cu LEFT JOIN FETCH cu.categoria")
    List<Usuario> findAllConCategorias();

    @Query("""
            SELECT DISTINCT u
            FROM Usuario u
            LEFT JOIN FETCH u.categoriasUsuarios cu
            LEFT JOIN FETCH cu.categoria
            WHERE u.estadoCuenta NOT IN :estadosNoPublicos
            """)
    List<Usuario> findAllConCategoriasByEstadoCuentaNotIn(@Param("estadosNoPublicos") List<EstadoCuenta> estadosNoPublicos);

    @Query(
            value = """
            SELECT DISTINCT u
            FROM Usuario u
            LEFT JOIN u.categoriasUsuarios cu
            LEFT JOIN cu.categoria c
            WHERE u.estadoCuenta NOT IN :estadosNoPublicos
              AND (
                    :q IS NULL
                    OR :q = ''
                    OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              AND (:idCategoria IS NULL OR c.idCategoria = :idCategoria)
              AND (
                    :categoria IS NULL
                    OR :categoria = ''
                    OR LOWER(c.nombreCategoria) = LOWER(:categoria)
                  )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT u.idUsuario)
            FROM Usuario u
            LEFT JOIN u.categoriasUsuarios cu
            LEFT JOIN cu.categoria c
            WHERE u.estadoCuenta NOT IN :estadosNoPublicos
              AND (
                    :q IS NULL
                    OR :q = ''
                    OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              AND (:idCategoria IS NULL OR c.idCategoria = :idCategoria)
              AND (
                    :categoria IS NULL
                    OR :categoria = ''
                    OR LOWER(c.nombreCategoria) = LOWER(:categoria)
                  )
            """
    )
    Page<Usuario> findArtistasPublicosPaginado(
            @Param("estadosNoPublicos") List<EstadoCuenta> estadosNoPublicos,
            @Param("q") String q,
            @Param("categoria") String categoria,
            @Param("idCategoria") Integer idCategoria,
            Pageable pageable
    );

    @Query(
            value = """
            SELECT DISTINCT u
            FROM Usuario u
            LEFT JOIN u.categoriasUsuarios cu
            LEFT JOIN cu.categoria c
            WHERE (
                    :q IS NULL
                    OR :q = ''
                    OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              AND (
                    :rol IS NULL
                    OR :rol = ''
                    OR UPPER(u.rol) = UPPER(:rol)
                  )
              AND (:estadoCuenta IS NULL OR u.estadoCuenta = :estadoCuenta)
              AND (:idCategoria IS NULL OR c.idCategoria = :idCategoria)
            """,
            countQuery = """
            SELECT COUNT(DISTINCT u.idUsuario)
            FROM Usuario u
            LEFT JOIN u.categoriasUsuarios cu
            LEFT JOIN cu.categoria c
            WHERE (
                    :q IS NULL
                    OR :q = ''
                    OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              AND (
                    :rol IS NULL
                    OR :rol = ''
                    OR UPPER(u.rol) = UPPER(:rol)
                  )
              AND (:estadoCuenta IS NULL OR u.estadoCuenta = :estadoCuenta)
              AND (:idCategoria IS NULL OR c.idCategoria = :idCategoria)
            """
    )
    Page<Usuario> findUsuariosPaginados(
            @Param("q") String q,
            @Param("rol") String rol,
            @Param("estadoCuenta") EstadoCuenta estadoCuenta,
            @Param("idCategoria") Integer idCategoria,
            Pageable pageable
    );

    List<Usuario> findByRolInAndEstadoCuenta(List<String> roles, EstadoCuenta estadoCuenta);
}
