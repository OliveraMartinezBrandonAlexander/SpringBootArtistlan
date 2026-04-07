package com.example.demo.service.impl;

import com.example.demo.dto.UsuarioDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CategoriaUsuariosRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.ServicioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private static final Set<String> ROLES_VALIDOS = Set.of("USER", "ADMIN", "MODERADOR");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-()\\s]{8,20}$");
    private static final int CATEGORIA_USUARIO_MIN = 19;
    private static final int CATEGORIA_USUARIO_MAX = 37;

    @Autowired
    private UsuarioRepository repo;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CategoriaUsuariosRepository categoriaUsuariosRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Override
    public Usuario guardarUsuario(Usuario u) {
        validarTelefono(u.getTelefono());
        return repo.save(u);
    }

    @Override
    public Usuario crearUsuarioConCategoria(UsuarioDTO dto) {
        validarTelefono(dto.getTelefono());
        Usuario usuario = convertirAEntidad(dto);
        Usuario guardado = repo.save(usuario);

        if (dto.getIdCategoria() != null && dto.getIdCategoria() > 0) {
            validarCategoriaUsuario(dto.getIdCategoria());
            if (guardado.getCategoriasUsuarios() == null || guardado.getCategoriasUsuarios().isEmpty()) {
                Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                        .orElseThrow(() -> new RuntimeException("Categoria no encontrada"));

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

    @Override
    public List<Usuario> todosUsuarios() {
        return repo.findAllConCategorias();
    }

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
            u.setUbicacion(datos.getUbicacion());
            u.setFechaNacimiento(datos.getFechaNacimiento());
            validarTelefono(u.getTelefono());
            return repo.save(u);
        });
    }

    @Transactional
    @Override
    public boolean eliminarUsuario(Integer id) {

        if (!repo.existsById(id)) {
            return false;
        }

        categoriaUsuariosRepository.deleteByUsuarioId(id);
        obraRepository.deleteByUsuarioId(id);
        servicioRepository.deleteByUsuarioId(id);

        repo.deleteById(id);

        return true;
    }

    @Override
    public Optional<Usuario> actualizarFotoPerfil(Integer id, String urlFoto) {
        return repo.findById(id).map(u -> {
            u.setFotoPerfil(urlFoto);
            return repo.save(u);
        });
    }

    @Override
    public Optional<Usuario> actualizarRol(Integer id, String nuevoRol, Integer adminId) {
        if (adminId == null) {
            throw new IllegalArgumentException("adminId es obligatorio para cambiar roles");
        }

        Usuario admin = repo.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario administrador no encontrado"));

        if (!"ADMIN".equalsIgnoreCase(admin.getRol())) {
            throw new SecurityException("Solo un ADMIN puede cambiar roles");
        }

        String rolNormalizado = nuevoRol == null ? "" : nuevoRol.trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rolNormalizado)) {
            throw new IllegalArgumentException("Rol invalido. Valores permitidos: USER, ADMIN, MODERADOR");
        }

        return repo.findById(id).map(usuario -> {
            usuario.setRol(rolNormalizado);
            return repo.save(usuario);
        });
    }

    @Override
    public Usuario actualizarUsuarioConCategoria(Integer id, UsuarioDTO dto) {

        Usuario usuario = repo.findByIdConCategorias(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Usuario no encontrado con ID: " + id));

        usuario = convertirAEntidad(dto, usuario);
        validarTelefono(usuario.getTelefono());

        if (dto.getIdCategoria() != null && dto.getIdCategoria() > 0) {
            validarCategoriaUsuario(dto.getIdCategoria());
            if (usuario.getCategoriasUsuarios() == null) {
                usuario.setCategoriasUsuarios(new HashSet<>());
            }
            usuario.getCategoriasUsuarios().clear();
            categoriaUsuariosRepository.deleteByUsuarioId(id);

            Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                    .orElseThrow(() -> new java.util.NoSuchElementException("Categoria no encontrada con ID: " + dto.getIdCategoria()));

            CategoriaUsuarios cu = new CategoriaUsuarios();
            CategoriaUsuariosID cuId = new CategoriaUsuariosID(usuario.getIdUsuario(), categoria.getIdCategoria());

            cu.setId(cuId);
            cu.setUsuario(usuario);
            cu.setCategoria(categoria);

            categoriaUsuariosRepository.save(cu);
            usuario.getCategoriasUsuarios().add(cu);
        }

        return repo.save(usuario);
    }

    private void validarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return;
        }
        if (!PHONE_PATTERN.matcher(telefono).matches()) {
            throw new BusinessException("Telefono invalido. Debe tener entre 8 y 20 caracteres y solo numeros/simbolos telefonicos.");
        }
    }

    private void validarCategoriaUsuario(Integer idCategoria) {
        if (idCategoria < CATEGORIA_USUARIO_MIN || idCategoria > CATEGORIA_USUARIO_MAX) {
            throw new BusinessException("La categoria de usuario debe estar entre 19 y 37.");
        }
    }

    @Override
    public List<Usuario> listarAdmins() {
        return repo.findAdmins();
    }

    private Usuario convertirAEntidad(UsuarioDTO dto) {
        Usuario u = new Usuario();
        u.setIdUsuario(dto.getIdUsuario());
        u.setNombreCompleto(dto.getNombreCompleto());
        u.setUsuario(dto.getUsuario());
        u.setContrasena(dto.getContrasena());
        u.setCorreo(dto.getCorreo());
        u.setDescripcion(dto.getDescripcion());
        u.setFotoPerfil(dto.getFotoPerfil());
        u.setTelefono(dto.getTelefono());
        u.setRedesSociales(dto.getRedesSociales());
        u.setUbicacion(dto.getUbicacion());
        u.setFechaNacimiento(dto.getFechaNacimiento());
        u.setRol("USER");
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
        u.setUbicacion(dto.getUbicacion());
        u.setFechaNacimiento(dto.getFechaNacimiento());
        u.setRol(usuarioExistente != null && usuarioExistente.getRol() != null
                ? usuarioExistente.getRol()
                : "USER");

        if (dto.getContrasena() != null && !dto.getContrasena().isEmpty()) {
            u.setContrasena(dto.getContrasena());
        }
        return u;
    }
}
