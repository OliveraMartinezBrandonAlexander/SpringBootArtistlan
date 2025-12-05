package com.example.demo.service.impl;

import com.example.demo.dto.UsuarioDTO;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CategoriaUsuariosRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository repo;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CategoriaUsuariosRepository categoriaUsuariosRepository;

    // Guardar usuario simple
    @Override
    public Usuario guardarUsuario(Usuario u) {
        return repo.save(u);
    }

    // Crear usuario y asignar categoría si viene en DTO
    @Override
    public Usuario crearUsuarioConCategoria(UsuarioDTO dto) {
        Usuario usuario = convertirAEntidad(dto);
        Usuario guardado = repo.save(usuario);

        if (dto.getIdCategoria() != null) {
            // Validar que no tenga otra categoría
            if (guardado.getCategoriasUsuarios() == null || guardado.getCategoriasUsuarios().isEmpty()) {
                Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                        .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

                CategoriaUsuarios cu = new CategoriaUsuarios();
                CategoriaUsuariosID id = new CategoriaUsuariosID(guardado.getIdUsuario(), categoria.getIdCategoria());
                cu.setId(id);
                cu.setUsuario(guardado);
                cu.setCategoria(categoria);

                categoriaUsuariosRepository.save(cu);
                guardado.getCategoriasUsuarios().add(cu);
            }
        }

        return guardado;
    }

    // Listar todos los usuarios con categorías (para módulo artistas)
    @Override
    public List<Usuario> todosUsuarios() {
        return repo.findAllConCategorias();
    }

    // Buscar usuario por id con categorías
    @Override
    public Optional<Usuario> buscarPorId(Integer id) {
        return repo.findByIdConCategorias(id);
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

    @Override
    public Optional<Usuario> actualizarFotoPerfil(Integer id, String urlFoto) {
        return repo.findById(id).map(u -> {
            u.setFotoPerfil(urlFoto);
            return repo.save(u);
        });
    }
    @Override
    public Usuario actualizarUsuarioConCategoria(Integer id, UsuarioDTO dto) {

        Usuario usuario = repo.findByIdConCategorias(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Usuario no encontrado con ID: " + id));

        usuario = convertirAEntidad(dto, usuario);

        if (dto.getIdCategoria() != null) {
            Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                    .orElseThrow(() -> new java.util.NoSuchElementException("Categoría no encontrada con ID: " + dto.getIdCategoria()));

            usuario.getCategoriasUsuarios().clear();

            CategoriaUsuarios cu = new CategoriaUsuarios();
            CategoriaUsuariosID cuId = new CategoriaUsuariosID(usuario.getIdUsuario(), categoria.getIdCategoria());

            cu.setId(cuId);
            cu.setUsuario(usuario);
            cu.setCategoria(categoria);

            usuario.getCategoriasUsuarios().add(cu);



        }
        return repo.save(usuario);
    }

    @Override
    public List<Usuario> listarAdmins() {
        return repo.findAdmins(); // solo admins
    }

    private Usuario convertirAEntidad(UsuarioDTO dto) {
        Usuario u = new Usuario();
        // No se setea el ID si es nuevo (o puede ser null, dependerá de tu DB)
        u.setIdUsuario(dto.getIdUsuario());
        u.setNombreCompleto(dto.getNombreCompleto());
        u.setUsuario(dto.getUsuario());
        u.setContrasena(dto.getContrasena());
        u.setCorreo(dto.getCorreo());
        u.setDescripcion(dto.getDescripcion());
        u.setFotoPerfil(dto.getFotoPerfil());
        u.setTelefono(dto.getTelefono());
        u.setRedesSociales(dto.getRedesSociales());
        u.setFechaNacimiento(dto.getFechaNacimiento());
        u.setAdminUsuario(dto.getAdminUsuario() == null ? 0 : dto.getAdminUsuario());
        return u;
    }

    private Usuario convertirAEntidad(UsuarioDTO dto, Usuario usuarioExistente) {

        Usuario u = usuarioExistente != null ? usuarioExistente : new Usuario();

        u.setIdUsuario(usuarioExistente != null ? usuarioExistente.getIdUsuario() : dto.getIdUsuario());

        u.setNombreCompleto(dto.getNombreCompleto());
        u.setUsuario(dto.getUsuario());
        u.setCorreo(dto.getCorreo());
        u.setDescripcion(dto.getDescripcion());
        u.setFotoPerfil(dto.getFotoPerfil());
        u.setTelefono(dto.getTelefono());
        u.setRedesSociales(dto.getRedesSociales());
        u.setFechaNacimiento(dto.getFechaNacimiento());
        u.setAdminUsuario(dto.getAdminUsuario() == null ? 0 : dto.getAdminUsuario());


        if (dto.getContrasena() != null && !dto.getContrasena().isEmpty()) {
            u.setContrasena(dto.getContrasena());
        }
        return u;
    }
}