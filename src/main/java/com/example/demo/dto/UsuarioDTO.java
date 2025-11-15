package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Date;

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
    private Date fechaNacimiento;
}