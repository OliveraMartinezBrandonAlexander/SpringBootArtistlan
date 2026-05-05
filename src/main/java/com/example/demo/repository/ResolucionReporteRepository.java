package com.example.demo.repository;

import com.example.demo.model.ResolucionReporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResolucionReporteRepository extends JpaRepository<ResolucionReporte, Integer> {

    boolean existsByReporte_IdReporte(Integer idReporte);

    Optional<ResolucionReporte> findByReporte_IdReporte(Integer idReporte);
}
