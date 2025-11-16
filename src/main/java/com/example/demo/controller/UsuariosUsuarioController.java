package com.example.demo.controller;

import com.example.demo.dto.UsuarioDTO;
import com.example.demo.model.Usuario;
import com.example.demo.service.UsuarioService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<UsuarioDTO> crearUsuario(@RequestBody Usuario usuario) {
        Usuario creado = usuarioService.guardarUsuario(usuario);
        return ResponseEntity.ok(convertirADTO(creado));
    }

    // PUT individual (actualizar)
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(@PathVariable Integer id, @RequestBody Usuario datos) {

        Usuario actualizado = usuarioService.actualizarUsuario(id, datos)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(convertirADTO(actualizado));
    }

    // DELETE individual
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Integer id) {

        boolean eliminado = usuarioService.eliminarUsuario(id);

        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

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
