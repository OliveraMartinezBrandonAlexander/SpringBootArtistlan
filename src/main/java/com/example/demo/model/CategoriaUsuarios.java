package com.example.demo.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "categoria_usuario")
@EqualsAndHashCode(callSuper = false, of = {"id"})
public class CategoriaUsuarios {

    @EmbeddedId
    private CategoriaUsuariosID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUsuario")
    @JoinColumn(name = "ID_USUARIO")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCategoria")
    @JoinColumn(name = "ID_CATEGORIA")
    private Categoria categoria;
}