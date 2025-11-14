package com.example.demo.model;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaUsuariosID implements Serializable {
    private Integer idUsuario;
    private Integer idCategoria;
}