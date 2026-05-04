package com.example.demo.model;

import com.example.demo.enums.AccionResolucionReporte;
import com.example.demo.enums.TipoRespuestaResolucion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resolucion_reporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolucionReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resolucion")
    private Integer idResolucion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reporte", nullable = false, unique = true)
    private Reporte reporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_moderador", nullable = false)
    private Usuario moderador;

    @Column(name = "mensaje_respuesta", nullable = false, columnDefinition = "TEXT")
    private String mensajeRespuesta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_respuesta", nullable = false, length = 30)
    private TipoRespuestaResolucion tipoRespuesta;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 50)
    private AccionResolucionReporte accion;

    @Column(name = "fecha_resolucion", nullable = false)
    private LocalDateTime fechaResolucion;

    @PrePersist
    public void prePersist() {
        if (fechaResolucion == null) {
            fechaResolucion = LocalDateTime.now();
        }
    }
}
