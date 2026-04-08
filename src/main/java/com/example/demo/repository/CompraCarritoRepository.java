package com.example.demo.repository;

import com.example.demo.model.CompraCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompraCarritoRepository extends JpaRepository<CompraCarrito, Integer> {

    Optional<CompraCarrito> findByPaypalOrderId(String paypalOrderId);

    @Query("""
            SELECT DISTINCT cc
            FROM CompraCarrito cc
            LEFT JOIN FETCH cc.comprador
            LEFT JOIN FETCH cc.detalles d
            LEFT JOIN FETCH d.obra o
            LEFT JOIN FETCH d.vendedor
            LEFT JOIN FETCH d.solicitud
            WHERE cc.paypalOrderId = :paypalOrderId
            """)
    Optional<CompraCarrito> findByPaypalOrderIdConDetalles(@Param("paypalOrderId") String paypalOrderId);

    List<CompraCarrito> findByCompradorIdUsuarioAndEstadoNot(Integer idUsuarioComprador, String estado);
}
