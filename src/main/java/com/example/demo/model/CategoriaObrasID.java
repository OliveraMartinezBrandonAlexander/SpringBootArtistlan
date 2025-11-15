package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaObrasID implements Serializable {

    @Column(name = "id_obra")
    private Integer idObra;

    @Column(name = "id_categoria")
    private Integer idCategoria;
}
