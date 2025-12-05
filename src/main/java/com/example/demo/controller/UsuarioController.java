package com.example.demo.controller;

import com.example.demo.dto.ActualizarFotoPerfilRequestDTO;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.UsuarioIdCategoriaDTO;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioIdCategoriaService;
import com.example.demo.service.UsuarioService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@AllArgsConstructor
@Validated
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioIdCategoriaService usuarioIdCategoriaService;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ---------------- GET TODOS USUARIOS ----------------
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerTodos() {
        List<Usuario> usuarios = usuarioService.todosUsuarios();
        if (usuarios.isEmpty()) return ResponseEntity.noContent().build();

        List<UsuarioDTO> dtos = usuarios.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ---------------- GET USUARIO POR ID ----------------
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerPorId(@PathVariable Integer id) {
        return usuarioRepository.findByIdConCategorias(id)
                .map(this::convertirADTO)

                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------- GET CATEGORIA DEL USUARIO ----------------
    @GetMapping("/{id}/categoria")
    public ResponseEntity<UsuarioDTO> obtenerCategoriaUsuario(@PathVariable Integer id) {
        return usuarioRepository.findByIdConCategorias(id)
                .map(u -> {
                    UsuarioDTO dto = convertirADTO(u);
                    dto.setContrasena(null);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------- POST CREAR USUARIOS ----------------
    @PostMapping
    public ResponseEntity<List<UsuarioDTO>> crearUsuarios(HttpServletRequest request) throws IOException {
        String body = request.getReader().lines().collect(Collectors.joining());

        List<UsuarioDTO> usuarios;
        if (body.trim().startsWith("[")) {
            usuarios = mapper.readValue(body, new TypeReference<List<UsuarioDTO>>() {});
        } else {
            UsuarioDTO u = mapper.readValue(body, UsuarioDTO.class);
            usuarios = Collections.singletonList(u);
        }

        List<UsuarioDTO> creados = usuarios.stream()
                .map(usuarioService::crearUsuarioConCategoria)
                .map(this::convertirADTO)
                .peek(dto -> dto.setContrasena(null))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(creados);
    }

    // ---------------- PUT EDITAR USUARIO ----------------
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> editarUsuario(@PathVariable Integer id, @RequestBody UsuarioDTO dto) {
        try {
            Usuario actualizado = usuarioService.actualizarUsuarioConCategoria(id, dto);
            return ResponseEntity.ok(convertirADTO(actualizado));

        } catch (java.util.NoSuchElementException e) {
            System.err.println("ERROR: Usuario o Categoría no encontrada: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();

        } catch (Exception e) {

            System.err.println("ERROR CRÍTICO AL ACTUALIZAR USUARIO:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ---------------- DELETE TODOS USUARIOS ----------------
    @DeleteMapping
    public ResponseEntity<Void> eliminarTodos() {
        usuarioService.todosUsuarios().forEach(u -> usuarioService.eliminarUsuario(u.getIdUsuario()));
        return ResponseEntity.noContent().build();
    }

    // ---------------- LOGIN ----------------
    @GetMapping("/login")
    public ResponseEntity<?> login(@RequestParam(required = false) String usuario,
                                   @RequestParam(required = false) String correo,
                                   @RequestParam String contrasena) {

        Optional<Usuario> user = Optional.empty();

        if (usuario != null && !usuario.isEmpty()) {
            user = usuarioRepository.findByUsuarioAndContrasena(usuario, contrasena);
        } else if (correo != null && !correo.isEmpty()) {
            user = usuarioRepository.findByCorreoAndContrasena(correo, contrasena);
        } else {
            return ResponseEntity.badRequest().body("Falta usuario o correo");
        }

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }

        UsuarioDTO dto = convertirADTO(user.get());
        dto.setContrasena(null);
        return ResponseEntity.ok(dto);
    }

    // ---------------- VERIFICAR EXISTENCIA ----------------
    @GetMapping("/existe")
    public ResponseEntity<String> existeUsuario(@RequestParam String usuario,
                                                @RequestParam String correo) {

        boolean usuarioExiste = usuarioRepository.existsByUsuario(usuario);
        boolean correoExiste = usuarioRepository.existsByCorreo(correo);

        if (usuarioExiste && correoExiste) return ResponseEntity.ok("AMBOS_DUPLICADOS");
        if (usuarioExiste) return ResponseEntity.ok("USUARIO_DUPLICADO");
        if (correoExiste) return ResponseEntity.ok("CORREO_DUPLICADO");
        return ResponseEntity.ok("OK");
    }

    // ---------------- ACTUALIZAR FOTO DE PERFIL ----------------
    @PutMapping("/{id}/foto-perfil")
    public ResponseEntity<UsuarioDTO> actualizarFotoPerfil(@PathVariable int id,
                                                           @RequestBody ActualizarFotoPerfilRequestDTO body) {
        Optional<Usuario> op = usuarioRepository.findById(id);
        if (op.isEmpty()) return ResponseEntity.notFound().build();

        Usuario usuario = op.get();
        usuario.setFotoPerfil(body.getFotoPerfil());

        Usuario actualizado = usuarioRepository.save(usuario);
        return ResponseEntity.ok(convertirADTO(actualizado));
    }

    // ---------------- CONVERSIONES ----------------
    private UsuarioDTO convertirADTO(Usuario u) {
        UsuarioDTO.UsuarioDTOBuilder builder = UsuarioDTO.builder()
                .idUsuario(u.getIdUsuario())
                .nombreCompleto(u.getNombreCompleto())
                .usuario(u.getUsuario())
                .contrasena(u.getContrasena())
                .correo(u.getCorreo())
                .descripcion(u.getDescripcion())
                .fotoPerfil(u.getFotoPerfil())
                .telefono(u.getTelefono())
                .redesSociales(u.getRedesSociales())
                .fechaNacimiento(u.getFechaNacimiento())
                .adminUsuario(u.getAdminUsuario())
                .idCategoria(null)
                .categoria(null);

        // Obtener categorías usando el service que ya sabe consultarlas
        List<UsuarioIdCategoriaDTO> categorias = usuarioIdCategoriaService.obtenerTodasCategoriasPorUsuario(u.getIdUsuario());
        if (!categorias.isEmpty()) {
            UsuarioIdCategoriaDTO cat = categorias.get(0); // solo la primera
            builder.idCategoria(cat.getIdCategoria());
            builder.categoria(cat.getNombreCategoria());
        }

        return builder.build();
    }

    private Usuario convertirAEntidad(UsuarioDTO dto, Usuario usuarioExistente) {
        Usuario u = usuarioExistente != null ? usuarioExistente : new Usuario();

        u.setIdUsuario(dto.getIdUsuario());
        u.setNombreCompleto(dto.getNombreCompleto());
        u.setUsuario(dto.getUsuario());
        u.setCorreo(dto.getCorreo());
        u.setDescripcion(dto.getDescripcion());
        u.setFotoPerfil(dto.getFotoPerfil());
        u.setTelefono(dto.getTelefono());
        u.setRedesSociales(dto.getRedesSociales());
        u.setFechaNacimiento(dto.getFechaNacimiento());
        u.setAdminUsuario(dto.getAdminUsuario() == null ? 0 : dto.getAdminUsuario());

        // Solo actualiza la contraseña si viene en el DTO
        if (dto.getContrasena() != null && !dto.getContrasena().isEmpty()) {
            u.setContrasena(dto.getContrasena());
        }

        return u;
    }
}