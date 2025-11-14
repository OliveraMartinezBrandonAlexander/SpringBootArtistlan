package com.example.demo.controller;

import com.example.demo.dto.UsuarioDTO;
import com.example.demo.model.Usuario;
import com.example.demo.service.UsuarioService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@AllArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

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

    // POST
    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(@RequestBody Usuario usuario) {
        Usuario guardado = usuarioService.guardarUsuario(usuario);
        return ResponseEntity.ok(convertirADTO(guardado));
    }

    // PUT todos
    @PutMapping
    public ResponseEntity<List<UsuarioDTO>> actualizarTodos(@RequestBody List<Usuario> usuarios) {
        List<UsuarioDTO> actualizados = usuarios.stream()
                .map(usuarioService::guardarUsuario)
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(actualizados);
    }

    // DELETE todos
    @DeleteMapping
    public ResponseEntity<Void> eliminarTodos() {
        usuarioService.todosUsuarios().forEach(u -> usuarioService.eliminarUsuario(u.getIdUsuario()));
        return ResponseEntity.noContent().build();
    }

    // Conversión a DTO
    private UsuarioDTO convertirADTO(Usuario u) {
        return UsuarioDTO.builder()
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
                .build();
    }
}