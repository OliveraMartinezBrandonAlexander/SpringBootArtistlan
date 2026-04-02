package com.example.demo.repository;

import com.example.demo.model.Favoritos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
