package com.example.demo.repository;

import com.example.demo.model.CompraObra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompraObraRepository extends JpaRepository<CompraObra, Integer> {

    Optional<CompraObra> findByPaypalOrderId(String paypalOrderId);

    boolean existsByObraIdObra(Integer idObra);

    boolean existsByObraIdObraAndEstado(Integer idObra, String estado);
}
