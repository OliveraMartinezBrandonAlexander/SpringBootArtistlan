package com.example.demo.service;

import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.moderacion.DesactivarCuentaRequestDTO;
import com.example.demo.dto.moderacion.RespuestaModeracionDTO;
import com.example.demo.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    Usuario guardarUsuario(Usuario u);

    Usuario crearUsuarioConCategoria(UsuarioDTO dto);

    Usuario actualizarUsuarioConCategoria(Integer id, UsuarioDTO dto);

    List<Usuario> todosUsuarios();

    Optional<Usuario> buscarPorId(Integer id);

    Optional<Usuario> actualizarUsuario(Integer id, Usuario datos);

    boolean eliminarUsuario(Integer id);

    Usuario validarCuentaPuedeAutenticarse(Usuario usuario);

    Usuario validarCuentaRecuperableParaPasswordReset(Usuario usuario);

    Optional<Usuario> buscarPorUsuario(String usuario);

    Optional<Usuario> buscarCuentaRecuperablePorUsuario(String usuario);

    Optional<Usuario> buscarPorUsuarioOCorreo(String usuarioOCorreo);

    Optional<Usuario> buscarCuentaRecuperablePorUsuarioOCorreo(String usuarioOCorreo);

    boolean validarContrasena(Usuario usuario, String contrasenaPlana);

    void validarNuevaContrasena(String nuevaContrasena, String confirmarContrasena);

    Usuario actualizarContrasenaPorRecuperacion(Usuario usuario, String nuevaContrasena);

    RespuestaModeracionDTO desactivarCuenta(Integer idUsuario, DesactivarCuentaRequestDTO request);

    List<Usuario> listarAdmins();

    Optional<Usuario> actualizarFotoPerfil(Integer id, String urlFoto);

    Optional<Usuario> actualizarRol(Integer id, String nuevoRol, Integer adminId);
}
