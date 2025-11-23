package com.example.demo.controller;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.model.Usuario;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@AllArgsConstructor
@Validated
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // GET todos
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerTodos() {
        List<Usuario> usuarios = usuarioService.todosUsuarios();
        if (usuarios.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<UsuarioDTO> dtos = usuarios.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // POST (recibe DTO, mapea a entidad y guarda)
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
                .map(this::convertirAEntidad)
                .map(usuarioService::guardarUsuario)
                .map(this::convertirADTO)
                .peek(dto -> dto.setContrasena(null))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(creados);
    }

    // PUT todos (recibe lista de DTOs, mapea, guarda y devuelve DTOs)
    @PutMapping
    public ResponseEntity<List<UsuarioDTO>> actualizarTodos(HttpServletRequest request) throws IOException {
        String body = request.getReader().lines().collect(Collectors.joining());
        List<UsuarioDTO> usuarios;
        if (body.trim().startsWith("[")) {
            usuarios = mapper.readValue(body, new TypeReference<List<UsuarioDTO>>() {});
        } else {
            UsuarioDTO u = mapper.readValue(body, UsuarioDTO.class);
            usuarios = Collections.singletonList(u);
        }
        List<UsuarioDTO> actualizados = usuarios.stream()
                .map(this::convertirAEntidad)
                .map(usuarioService::guardarUsuario)
                .map(this::convertirADTO)
                .peek(dto -> dto.setContrasena(null))
                .collect(Collectors.toList());
        return ResponseEntity.ok(actualizados);
    }

    // DELETE todos
    @DeleteMapping
    public ResponseEntity<Void> eliminarTodos() {
        usuarioService.todosUsuarios().forEach(u -> usuarioService.eliminarUsuario(u.getIdUsuario()));
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam String usuario,
            @RequestParam String correo,
            @RequestParam String contrasena
    ) {
        Optional<Usuario> user = usuarioRepository
                .findByUsuarioAndCorreoAndContrasena(usuario, correo, contrasena);

        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales incorrectas");
        }
    }

    // Conversión entidad -> DTO
    private UsuarioDTO convertirADTO(Usuario u) {
        return UsuarioDTO.builder()
                .idUsuario(u.getIdUsuario())
                .nombreCompleto(u.getNombreCompleto())
                .usuario(u.getUsuario())
                .contrasena(null)
                .correo(u.getCorreo())
                .descripcion(u.getDescripcion())
                .fotoPerfil(u.getFotoPerfil())
                .telefono(u.getTelefono())
                .redesSociales(u.getRedesSociales())
                .fechaNacimiento(u.getFechaNacimiento())
                .adminUsuario(u.getAdminUsuario())
                .build();
    }

    // Conversión DTO -> entidad
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
        u.setFechaNacimiento(dto.getFechaNacimiento());
        u.setAdminUsuario(dto.getAdminUsuario() == null ? 0 : dto.getAdminUsuario());
        return u;
    }
}
