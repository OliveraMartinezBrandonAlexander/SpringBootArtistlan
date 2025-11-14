package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categoria_obra")
@Data
public class CategoriaObras {

    @EmbeddedId
    private CategoriaObrasID id;

    @ManyToOne
    @MapsId("idObra")
    @JoinColumn(name = "id_obra")
    private Obra obra;

    @ManyToOne
    @MapsId("idCategoria")
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;
}
