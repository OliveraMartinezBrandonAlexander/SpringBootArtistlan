package com.example.demo.repository;

import com.example.demo.model.CompraCarritoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompraCarritoDetalleRepository extends JpaRepository<CompraCarritoDetalle, Integer> {

    @Query("""
            SELECT DISTINCT d
            FROM CompraCarritoDetalle d
            JOIN FETCH d.compraCarrito cc
            JOIN FETCH cc.comprador comprador
            JOIN FETCH d.obra o
            JOIN FETCH d.vendedor vendedor
            LEFT JOIN FETCH o.usuario
            WHERE comprador.idUsuario = :idUsuario
              AND cc.estado = :estado
            """)
    List<CompraCarritoDetalle> findComprasCarritoByCompradorId(@Param("idUsuario") Integer idUsuario,
                                                               @Param("estado") String estado);

    @Query("""
            SELECT DISTINCT d
            FROM CompraCarritoDetalle d
            JOIN FETCH d.compraCarrito cc
            JOIN FETCH cc.comprador comprador
            JOIN FETCH d.obra o
            JOIN FETCH d.vendedor vendedor
            LEFT JOIN FETCH o.usuario
            WHERE vendedor.idUsuario = :idUsuario
              AND cc.estado = :estado
            """)
    List<CompraCarritoDetalle> findVentasCarritoByVendedorId(@Param("idUsuario") Integer idUsuario,
                                                             @Param("estado") String estado);

    boolean existsByObraIdObra(Integer idObra);

    boolean existsByObraIdObraAndCompraCarritoEstado(Integer idObra, String estado);

    @Modifying
    @Query("""
            DELETE FROM CompraCarritoDetalle d
            WHERE d.obra.idObra = :idObra
              AND d.compraCarrito.estado <> :estadoVentaReal
            """)
    int deleteNoVendidasByObraId(@Param("idObra") Integer idObra,
                                 @Param("estadoVentaReal") String estadoVentaReal);
}
