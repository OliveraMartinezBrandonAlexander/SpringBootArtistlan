package com.example.demo.model;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaObrasID implements Serializable {
    private Integer idObra;
    private Integer idCategoria;
}