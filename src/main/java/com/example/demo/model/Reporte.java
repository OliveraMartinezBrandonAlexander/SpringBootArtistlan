package com.example.demo.model;

import com.example.demo.enums.EstadoReporte;
import com.example.demo.enums.PrioridadReporte;
import com.example.demo.enums.TipoObjetivoReporte;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_reportante", nullable = false)
    private Usuario usuarioReportante;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_objetivo", nullable = false, length = 20)
    private TipoObjetivoReporte tipoObjetivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_obra")
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_reportado")
    private Usuario usuarioReportado;

    @Column(name = "motivo", nullable = false, length = 100)
    private String motivo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoReporte estado = EstadoReporte.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 20)
    @Builder.Default
    private PrioridadReporte prioridad = PrioridadReporte.MEDIA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_moderador_asignado")
    private Usuario moderadorAsignado;

    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    @Column(name = "fecha_inicio_revision")
    private LocalDateTime fechaInicioRevision;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (estado == null) {
            estado = EstadoReporte.PENDIENTE;
        }
        if (prioridad == null) {
            prioridad = PrioridadReporte.MEDIA;
        }
        if (fechaReporte == null) {
            fechaReporte = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
