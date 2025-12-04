package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria_servicio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaServicios {

    @EmbeddedId
    private CategoriaServiciosID id;

    @ManyToOne
    @MapsId("idServicio")
    @JoinColumn(name = "id_servicio")
    @JsonIgnore
    private Servicio servicio;

    @ManyToOne
    @MapsId("idCategoria")
    @JoinColumn(name = "id_categoria")
    @JsonBackReference
    private Categoria categoria;
}
