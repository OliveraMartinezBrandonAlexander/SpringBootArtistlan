package com.example.demo.repository;

import com.example.demo.enums.EstadoMetaPersonal;
import com.example.demo.enums.TipoMetaPersonal;
import com.example.demo.model.MetaPersonalArtista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;

public interface MetaPersonalArtistaRepository extends JpaRepository<MetaPersonalArtista, Integer> {

    List<MetaPersonalArtista> findByUsuarioIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);

    boolean existsByUsuarioIdUsuarioAndTipoMetaAndEstadoIn(
            Integer idUsuario,
            TipoMetaPersonal tipoMeta,
            Collection<EstadoMetaPersonal> estados
    );

    boolean existsByUsuarioIdUsuarioAndTipoMetaAndEstadoInAndIdMetaNot(
            Integer idUsuario,
            TipoMetaPersonal tipoMeta,
            Collection<EstadoMetaPersonal> estados,
            Integer idMeta
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM MetaPersonalArtista m
            WHERE m.usuario.idUsuario = :idUsuario
              AND m.estado IN :estados
              AND m.tipoMeta IN :tipos
            ORDER BY m.fechaCreacion DESC
            """)
    List<MetaPersonalArtista> findActivasByUsuarioYTiposForUpdate(
            @Param("idUsuario") Integer idUsuario,
            @Param("estados") Collection<EstadoMetaPersonal> estados,
            @Param("tipos") Collection<TipoMetaPersonal> tipos
    );
}
