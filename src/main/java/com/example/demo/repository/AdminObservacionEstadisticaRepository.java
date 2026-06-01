package com.example.demo.repository;

import com.example.demo.enums.AdminTipoDatoEstadistica;
import com.example.demo.enums.AdminTipoEstadistica;
import com.example.demo.model.AdminObservacionEstadistica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdminObservacionEstadisticaRepository extends JpaRepository<AdminObservacionEstadistica, Integer> {

    @Query("""
            SELECT o
            FROM AdminObservacionEstadistica o
            JOIN FETCH o.admin a
            WHERE o.idObservacion = :idObservacion
            """)
    Optional<AdminObservacionEstadistica> findDetalleById(@Param("idObservacion") Integer idObservacion);

    @Query("""
            SELECT o
            FROM AdminObservacionEstadistica o
            JOIN FETCH o.admin a
            WHERE (:tipoEstadistica IS NULL OR o.tipoEstadistica = :tipoEstadistica)
              AND (:tipoDato IS NULL OR o.tipoDato = :tipoDato)
              AND (:fechaInicioPeriodo IS NULL OR o.fechaInicioPeriodo = :fechaInicioPeriodo)
              AND (:fechaFinPeriodo IS NULL OR o.fechaFinPeriodo = :fechaFinPeriodo)
            ORDER BY o.fechaActualizacion DESC, o.idObservacion DESC
            """)
    List<AdminObservacionEstadistica> findAllByFiltros(
            @Param("tipoEstadistica") AdminTipoEstadistica tipoEstadistica,
            @Param("tipoDato") AdminTipoDatoEstadistica tipoDato,
            @Param("fechaInicioPeriodo") LocalDate fechaInicioPeriodo,
            @Param("fechaFinPeriodo") LocalDate fechaFinPeriodo
    );

    @Query("""
            SELECT o
            FROM AdminObservacionEstadistica o
            WHERE o.tipoEstadistica = :tipoEstadistica
              AND o.tipoDato = :tipoDato
              AND ((:fechaInicioPeriodo IS NULL AND o.fechaInicioPeriodo IS NULL) OR o.fechaInicioPeriodo = :fechaInicioPeriodo)
              AND ((:fechaFinPeriodo IS NULL AND o.fechaFinPeriodo IS NULL) OR o.fechaFinPeriodo = :fechaFinPeriodo)
            """)
    Optional<AdminObservacionEstadistica> findByContexto(
            @Param("tipoEstadistica") AdminTipoEstadistica tipoEstadistica,
            @Param("tipoDato") AdminTipoDatoEstadistica tipoDato,
            @Param("fechaInicioPeriodo") LocalDate fechaInicioPeriodo,
            @Param("fechaFinPeriodo") LocalDate fechaFinPeriodo
    );
}
