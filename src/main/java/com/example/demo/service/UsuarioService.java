package com.example.demo.service;
import com.example.demo.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    Usuario guardarUsuario(Usuario u);
    List<Usuario> todosUsuarios();
    Optional<Usuario> buscarPorId(Integer id);
    Optional<Usuario> actualizarUsuario(Integer id, Usuario datos);
    boolean eliminarUsuario(Integer id);
    List<Usuario> listarAdmins();

}
