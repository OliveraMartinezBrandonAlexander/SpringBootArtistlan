package com.example.demo.model;

import com.example.demo.enums.EstadoSuspensionUsuario;
import com.example.demo.enums.TipoSancionUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "suspension_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspensionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suspension")
    private Integer idSuspension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_moderador", nullable = false)
    private Usuario moderador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reporte")
    private Reporte reporte;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sancion", nullable = false, length = 40)
    private TipoSancionUsuario tipoSancion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoSuspensionUsuario estado = EstadoSuspensionUsuario.ACTIVA;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion;

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (estado == null) {
            estado = EstadoSuspensionUsuario.ACTIVA;
        }
        if (fechaInicio == null) {
            fechaInicio = ahora;
        }
        if (fechaAccion == null) {
            fechaAccion = ahora;
        }
    }
}
