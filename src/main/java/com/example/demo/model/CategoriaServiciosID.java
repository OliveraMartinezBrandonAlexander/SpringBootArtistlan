package com.example.demo.model;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaServiciosID implements Serializable {
    private Integer idServicio;
    private Integer idCategoria;
}
