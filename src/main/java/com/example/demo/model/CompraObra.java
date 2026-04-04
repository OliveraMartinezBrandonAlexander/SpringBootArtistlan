package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "compra_obra",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_compra_obra_paypal_order", columnNames = "paypal_order_id"),
                @UniqueConstraint(name = "uq_compra_obra_paypal_capture", columnNames = "paypal_capture_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraObra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Integer idCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_obra", nullable = false)
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_comprador", nullable = false)
    private Usuario comprador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

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

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
