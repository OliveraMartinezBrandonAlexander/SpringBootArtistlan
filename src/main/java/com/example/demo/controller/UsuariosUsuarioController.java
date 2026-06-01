package com.example.demo.controller;

import com.example.demo.dto.UsuarioDTO;
import com.example.demo.model.Usuario;
import com.example.demo.service.UsuarioService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;

@RestController
@RequestMapping("/api/usuariosusuario")
@AllArgsConstructor
public class UsuariosUsuarioController {

    private final UsuarioService usuarioService;

    // GET individual
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuario(@PathVariable Integer id) {

        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(convertirADTO(usuario));
    }

    // POST individual (crear)
    @PostMapping
    public ResponseEntity<Map<String, String>> crearUsuario() {
        return ResponseEntity.status(METHOD_NOT_ALLOWED)
                .body(Map.of("message", "Operacion no disponible en el modulo legacy de usuarios."));
    }

    // PUT individual (actualizar)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> actualizarUsuario(@PathVariable Integer id) {
        return ResponseEntity.status(METHOD_NOT_ALLOWED)
                .body(Map.of("message", "Operacion no disponible en el modulo legacy de usuarios."));
    }

    // DELETE individual
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarUsuario(@PathVariable Integer id) {
        return ResponseEntity.status(METHOD_NOT_ALLOWED)
                .body(Map.of("message", "Operacion no disponible en el modulo legacy de usuarios."));
    }

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
                .ubicacion(u.getUbicacion())
                .fechaNacimiento(u.getFechaNacimiento())
                .twoFactorEnabled(Boolean.TRUE.equals(u.getTwoFactorEnabled()))
                .build();
    }
}
