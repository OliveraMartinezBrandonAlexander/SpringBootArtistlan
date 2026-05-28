package com.example.demo.model;

import com.example.demo.enums.EstadoModeracion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "servicio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer idServicio;

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "tipo_contacto", nullable = false, length = 20)
    private String tipoContacto;

    @Column(name = "contacto", nullable = false, length = 150)
    private String contacto;

    @Column(name = "oculto", nullable = false)
    @Builder.Default
    private Boolean oculto = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_moderacion", nullable = false, length = 40)
    @Builder.Default
    private EstadoModeracion estadoModeracion = EstadoModeracion.SIN_REPORTES;

    @Column(name = "motivo_oculto", columnDefinition = "TEXT")
    private String motivoOculto;

    @Column(name = "fecha_oculto")
    private LocalDateTime fechaOculto;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Column(name = "tecnicas", length = 255)
    private String tecnicas;

    @Column(name = "precio_min", precision = 10, scale = 2)
    private BigDecimal precioMin;

    @Column(name = "precio_max", precision = 10, scale = 2)
    private BigDecimal precioMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoriaServicios> categoriasServicios = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (fechaPublicacion == null) {
            fechaPublicacion = LocalDateTime.now();
        }
    }
}
