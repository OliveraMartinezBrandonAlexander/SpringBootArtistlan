package com.example.demo.model;

import com.example.demo.enums.AccionModeracionContenido;
import com.example.demo.enums.TipoContenidoModerado;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "moderacion_contenido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeracionContenido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_moderacion")
    private Integer idModeracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reporte")
    private Reporte reporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contenido", nullable = false, length = 20)
    private TipoContenidoModerado tipoContenido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_obra")
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_moderador", nullable = false)
    private Usuario moderador;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 30)
    private AccionModeracionContenido accion;

    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion;

    @PrePersist
    public void prePersist() {
        if (fechaAccion == null) {
            fechaAccion = LocalDateTime.now();
        }
    }
}
