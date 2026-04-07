package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "obra")
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_obra")
    private Integer idObra;

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "imagen1", nullable = false, length = 500)
    private String imagen1;

    @Column(name = "imagen2", length = 500)
    private String imagen2;

    @Column(name = "imagen3", length = 500)
    private String imagen3;

    @Column(name = "tecnicas", length = 255)
    private String tecnicas;

    @Column(name = "medidas", length = 100)
    private String medidas;

    @Column(name = "confirmacion_autoria", nullable = false)
    @Builder.Default
    private Boolean confirmacionAutoria = Boolean.FALSE;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<CategoriaObras> categoriaObras = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (fechaPublicacion == null) {
            fechaPublicacion = LocalDateTime.now();
        }
    }
}
