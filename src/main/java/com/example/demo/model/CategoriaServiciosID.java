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
public class CategoriaServiciosID implements Serializable {

    @Column(name = "id_servicio")
    private Integer idServicio;

    @Column(name = "id_categoria")
    private Integer idCategoria;
}
