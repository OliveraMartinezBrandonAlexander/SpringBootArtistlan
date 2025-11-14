package com.example.demo.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categoria_usuario")
@Data
public class CategoriaUsuarios {

    @EmbeddedId
    private CategoriaUsuariosID id;

    @ManyToOne
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne
    @MapsId("idCategoria")
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;
}