package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "carrito",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_carrito_usuario_obra", columnNames = {"id_usuario", "id_obra"}),
                @UniqueConstraint(name = "uq_carrito_obra", columnNames = {"id_obra"}),
                @UniqueConstraint(name = "uq_carrito_solicitud", columnNames = {"id_solicitud"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrito")
    private Integer idCarrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_obra", nullable = false)
    private Obra obra;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private SolicitudCompraObra solicitud;

    @Column(name = "fecha_agregado", nullable = false, updatable = false)
    private LocalDateTime fechaAgregado;

    @Column(name = "reservada_hasta", nullable = false)
    private LocalDateTime reservadaHasta;

    @PrePersist
    public void prePersist() {
        if (fechaAgregado == null) {
            fechaAgregado = LocalDateTime.now();
        }
    }
}
