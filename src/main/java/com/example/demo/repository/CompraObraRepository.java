package com.example.demo.repository;

import com.example.demo.model.CompraObra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompraObraRepository extends JpaRepository<CompraObra, Integer> {

    Optional<CompraObra> findByPaypalOrderId(String paypalOrderId);

    Optional<CompraObra> findBySolicitudIdSolicitud(Integer idSolicitud);

    @Query("""
            SELECT co
            FROM CompraObra co
            LEFT JOIN FETCH co.obra o
            LEFT JOIN FETCH co.comprador c
            LEFT JOIN FETCH co.vendedor v
            LEFT JOIN FETCH co.solicitud
            LEFT JOIN FETCH o.usuario
            WHERE co.idCompra = :idCompra
            """)
    Optional<CompraObra> findByIdDetallada(@Param("idCompra") Integer idCompra);

    boolean existsBySolicitudIdSolicitud(Integer idSolicitud);

    @Query("""
            SELECT co
            FROM CompraObra co
            JOIN FETCH co.obra o
            JOIN FETCH co.comprador c
            JOIN FETCH co.vendedor v
            LEFT JOIN FETCH o.usuario
            WHERE c.idUsuario = :idUsuario
              AND co.estado = :estado
            """)
    List<CompraObra> findComprasDirectasByCompradorId(@Param("idUsuario") Integer idUsuario,
                                                      @Param("estado") String estado);

    @Query("""
            SELECT co
            FROM CompraObra co
            JOIN FETCH co.obra o
            JOIN FETCH co.comprador c
            JOIN FETCH co.vendedor v
            LEFT JOIN FETCH o.usuario
            WHERE v.idUsuario = :idUsuario
              AND co.estado = :estado
            """)
    List<CompraObra> findVentasDirectasByVendedorId(@Param("idUsuario") Integer idUsuario,
                                                    @Param("estado") String estado);

    boolean existsByObraIdObra(Integer idObra);

    boolean existsByObraIdObraAndEstado(Integer idObra, String estado);

    @Modifying
    @Query("""
            DELETE FROM CompraObra co
            WHERE co.obra.idObra = :idObra
              AND co.estado <> :estadoVentaReal
            """)
    int deleteNoVendidasByObraId(@Param("idObra") Integer idObra,
                                 @Param("estadoVentaReal") String estadoVentaReal);

    @Query("""
            SELECT COUNT(co)
            FROM CompraObra co
            WHERE co.vendedor.idUsuario = :idUsuario
              AND UPPER(co.estado) IN :estados
              AND COALESCE(co.fechaCaptura, co.fechaCreacion) BETWEEN :inicio AND :fin
            """)
    long countVentasCompletadasByVendedorYPeriodo(@Param("idUsuario") Integer idUsuario,
                                                  @Param("estados") List<String> estados,
                                                  @Param("inicio") LocalDateTime inicio,
                                                  @Param("fin") LocalDateTime fin);

    @Query("""
            SELECT SUM(co.monto)
            FROM CompraObra co
            WHERE co.vendedor.idUsuario = :idUsuario
              AND UPPER(co.estado) IN :estados
              AND COALESCE(co.fechaCaptura, co.fechaCreacion) BETWEEN :inicio AND :fin
            """)
    BigDecimal sumIngresosCompletadosByVendedorYPeriodo(@Param("idUsuario") Integer idUsuario,
                                                        @Param("estados") List<String> estados,
                                                        @Param("inicio") LocalDateTime inicio,
                                                        @Param("fin") LocalDateTime fin);
}
