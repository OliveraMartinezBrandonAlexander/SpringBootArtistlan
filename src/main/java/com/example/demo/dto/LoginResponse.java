package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Boolean login;
    private Boolean requires2FA;
    private String token;
    private UsuarioDTO user;
    private String temporaryToken;

    // Campos legacy para mantener compatibilidad con clientes actuales.
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

    public void aplicarCamposLegacyDesdeUsuario(UsuarioDTO usuarioDTO) {
        if (usuarioDTO == null) {
            return;
        }
        this.idUsuario = usuarioDTO.getIdUsuario();
        this.nombreCompleto = usuarioDTO.getNombreCompleto();
        this.contrasena = usuarioDTO.getContrasena();
        this.usuario = usuarioDTO.getUsuario();
        this.correo = usuarioDTO.getCorreo();
        this.descripcion = usuarioDTO.getDescripcion();
        this.fotoPerfil = usuarioDTO.getFotoPerfil();
        this.telefono = usuarioDTO.getTelefono();
        this.redesSociales = usuarioDTO.getRedesSociales();
        this.ubicacion = usuarioDTO.getUbicacion();
        this.fechaNacimiento = usuarioDTO.getFechaNacimiento();
        this.rol = usuarioDTO.getRol();
        this.likes = usuarioDTO.getLikes();
        this.esFavorito = usuarioDTO.getEsFavorito();
        this.idCategoria = usuarioDTO.getIdCategoria();
        this.categoria = usuarioDTO.getCategoria();
    }
}
