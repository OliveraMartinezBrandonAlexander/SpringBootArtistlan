package com.example.demo.repository;

import com.example.demo.model.Favoritos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.enums.EstadoModeracion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface FavoritosRepository extends JpaRepository<Favoritos, Integer> {

    @Query("SELECT COUNT(f) FROM Favoritos f WHERE f.obra.idObra = :idObra")
    long countByIdObra(@Param("idObra") Long idObra);

    @Query("SELECT COUNT(f) FROM Favoritos f WHERE f.servicio.idServicio = :idServicio")
    long countByIdServicio(@Param("idServicio") Long idServicio);

    @Query("SELECT COUNT(f) FROM Favoritos f WHERE f.artista.idUsuario = :idArtista")
    long countByIdArtista(@Param("idArtista") Long idArtista);

    @Query("SELECT f FROM Favoritos f WHERE f.usuario.idUsuario = :idUsuario")
    List<Favoritos> findByIdUsuario(@Param("idUsuario") Long idUsuario);

    boolean existsByUsuarioIdUsuarioAndObraIdObra(Integer idUsuario, Integer idObra);

    boolean existsByUsuarioIdUsuarioAndServicioIdServicio(Integer idUsuario, Integer idServicio);

    boolean existsByUsuarioIdUsuarioAndArtistaIdUsuario(Integer idUsuario, Integer idArtista);

    Optional<Favoritos> findByUsuarioIdUsuarioAndObraIdObra(Integer idUsuario, Integer idObra);

    Optional<Favoritos> findByUsuarioIdUsuarioAndServicioIdServicio(Integer idUsuario, Integer idServicio);

    Optional<Favoritos> findByUsuarioIdUsuarioAndArtistaIdUsuario(Integer idUsuario, Integer idArtista);

    boolean existsByObraIdObra(Integer idObra);

    boolean existsByServicioIdServicio(Integer idServicio);

    @Modifying
    void deleteByObraIdObra(Integer idObra);

    @Modifying
    void deleteByServicioIdServicio(Integer idServicio);

    @Query("""
            SELECT COUNT(f)
            FROM Favoritos f
            WHERE f.artista.idUsuario = :idUsuario
              AND f.fecha BETWEEN :inicio AND :fin
            """)
    long countFavoritosDirectosRecibidosEnPeriodo(@Param("idUsuario") Integer idUsuario,
                                                  @Param("inicio") LocalDateTime inicio,
                                                  @Param("fin") LocalDateTime fin);

    @Query("""
            SELECT COUNT(f)
            FROM Favoritos f
            JOIN f.obra o
            WHERE o.usuario.idUsuario = :idUsuario
              AND f.fecha BETWEEN :inicio AND :fin
              AND COALESCE(o.oculta, false) = false
              AND o.estadoModeracion NOT IN :estadosExcluidos
            """)
    long countFavoritosObrasVisiblesRecibidosEnPeriodo(@Param("idUsuario") Integer idUsuario,
                                                       @Param("inicio") LocalDateTime inicio,
                                                       @Param("fin") LocalDateTime fin,
                                                       @Param("estadosExcluidos") List<EstadoModeracion> estadosExcluidos);

    @Query("""
            SELECT COUNT(f)
            FROM Favoritos f
            JOIN f.servicio s
            WHERE s.usuario.idUsuario = :idUsuario
              AND f.fecha BETWEEN :inicio AND :fin
              AND COALESCE(s.oculto, false) = false
              AND s.estadoModeracion NOT IN :estadosExcluidos
            """)
    long countFavoritosServiciosVisiblesRecibidosEnPeriodo(@Param("idUsuario") Integer idUsuario,
                                                           @Param("inicio") LocalDateTime inicio,
                                                           @Param("fin") LocalDateTime fin,
                                                           @Param("estadosExcluidos") List<EstadoModeracion> estadosExcluidos);
}
