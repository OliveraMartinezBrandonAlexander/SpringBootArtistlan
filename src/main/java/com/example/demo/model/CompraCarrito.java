package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "compra_carrito",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_compra_carrito_paypal_order", columnNames = "paypal_order_id"),
                @UniqueConstraint(name = "uq_compra_carrito_paypal_capture", columnNames = "paypal_capture_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra_carrito")
    private Integer idCompraCarrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_comprador", nullable = false)
    private Usuario comprador;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "moneda", nullable = false, length = 10)
    private String moneda;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "paypal_order_id", nullable = false, length = 100)
    private String paypalOrderId;

    @Column(name = "paypal_capture_id", length = 100)
    private String paypalCaptureId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_captura")
    private LocalDateTime fechaCaptura;

    @OneToMany(mappedBy = "compraCarrito", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CompraCarritoDetalle> detalles = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
