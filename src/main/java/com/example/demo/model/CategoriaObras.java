package com.example.demo.model;

import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaObrasID;
import com.example.demo.model.Obra;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "categoria_obra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoriaObras)) return false;
        CategoriaObras that = (CategoriaObras) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}