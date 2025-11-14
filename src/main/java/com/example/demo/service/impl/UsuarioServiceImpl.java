package com.example.demo.service.impl;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository repo;

    @Override
    public Usuario guardarUsuario(Usuario u) {
        return repo.save(u);
    }

    @Override
    public List<Usuario> todosUsuarios() {
        return repo.findAll();
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer id) {
        return repo.findById(id);
    }

    @Override
    public Optional<Usuario> actualizarUsuario(Integer id, Usuario datos) {
        return repo.findById(id).map(u -> {
            u.setNombreCompleto(datos.getNombreCompleto());
            u.setUsuario(datos.getUsuario());
            u.setCorreo(datos.getCorreo());
            u.setContrasena(datos.getContrasena());
            u.setDescripcion(datos.getDescripcion());
            u.setFotoPerfil(datos.getFotoPerfil());
            u.setTelefono(datos.getTelefono());
            u.setRedesSociales(datos.getRedesSociales());
            u.setFechaNacimiento(datos.getFechaNacimiento());
            u.setAdminUsuario(datos.getAdminUsuario());
            return repo.save(u);
        });
    }

    @Override
    public boolean eliminarUsuario(Integer id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
            return true;
        }
        return false;
    }
}
