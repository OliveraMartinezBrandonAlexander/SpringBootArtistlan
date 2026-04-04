package com.example.demo.repository;

import com.example.demo.model.CompraCarritoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraCarritoDetalleRepository extends JpaRepository<CompraCarritoDetalle, Integer> {

    boolean existsByObraIdObraAndCompraCarritoEstado(Integer idObra, String estado);
}
