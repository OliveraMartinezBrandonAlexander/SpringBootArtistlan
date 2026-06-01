package com.example.demo.model;

import com.example.demo.enums.AdminTipoDatoEstadistica;
import com.example.demo.enums.AdminTipoEstadistica;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_observacion_estadistica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminObservacionEstadistica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_observacion")
    private Integer idObservacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin", nullable = false)
    private Usuario admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_estadistica", nullable = false, length = 50)
    private AdminTipoEstadistica tipoEstadistica;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dato", nullable = false, length = 50)
    private AdminTipoDatoEstadistica tipoDato;

    @Column(name = "fecha_inicio_periodo")
    private LocalDate fechaInicioPeriodo;

    @Column(name = "fecha_fin_periodo")
    private LocalDate fechaFinPeriodo;

    @Column(name = "observacion", nullable = false, columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaActualizacion = ahora;
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
