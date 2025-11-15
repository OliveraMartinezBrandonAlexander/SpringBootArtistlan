package com.example.demo.model;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaUsuariosID implements Serializable {
    @Column(name = "id_usuario")
    private Integer idUsuario;
    @Column(name = "id_categoria")
    private Integer idCategoria;
}