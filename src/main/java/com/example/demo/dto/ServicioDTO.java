package com.example.demo.dto;

import com.example.demo.model.Servicio;
import com.example.demo.model.CategoriaServicios;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioDTO {

    private Integer idServicio;
    private String titulo;
    private String descripcion;
    private String contacto;
    private String tecnicas;

    private Integer idUsuario;
    private String nombreUsuario;

    private Integer idCategoria;
    private String categoria;

    public ServicioDTO(Servicio s) {
        this.idServicio = s.getIdServicio();
        this.titulo = s.getTitulo();
        this.descripcion = s.getDescripcion();
        this.contacto = s.getContacto();
        this.tecnicas = s.getTecnicas();

        this.idUsuario = s.getUsuario().getIdUsuario();
        this.nombreUsuario = s.getUsuario().getUsuario();

        if (s.getCategoriasServicios() != null && !s.getCategoriasServicios().isEmpty()) {

            CategoriaServicios cs = s.getCategoriasServicios().iterator().next();

            if (cs.getCategoria() != null) {
                this.idCategoria = cs.getCategoria().getIdCategoria();
                this.categoria = cs.getCategoria().getNombreCategoria();
            }
        }
    }
}