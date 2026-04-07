package com.example.demo.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private Integer idUsuario;
    private String nombreCompleto;
    private String contrasena;
    private String usuario;
    private String correo;
    private String descripcion;
    private String fotoPerfil;
    private String telefono;
    private String redesSociales;
    private String ubicacion;
    private LocalDate fechaNacimiento;
    private String rol;
    private Integer likes;
    private Boolean esFavorito;

    private Integer idCategoria;
    private String categoria;
}
