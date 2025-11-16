package com.example.demo.controller;

import com.example.demo.dto.FavoritosDTO;
import com.example.demo.model.Favoritos;
import com.example.demo.model.Obra;
import com.example.demo.model.Servicio;
import com.example.demo.model.Usuario;
import com.example.demo.service.FavoritosService;
import com.example.demo.service.ObraService;
import com.example.demo.service.ServicioService;
import com.example.demo.service.UsuarioService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios/favoritos")
@AllArgsConstructor
public class UsuarioFavoritosController {

    private final FavoritosService favoritosService;
    private final UsuarioService usuarioService;
    private final ObraService obraService;
    private final ServicioService servicioService;

    // GET - Obtener todos los favoritos de un usuario específico
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<FavoritosDTO>> obtenerFavoritosPorUsuario(@PathVariable Integer usuarioId) {
        // Verificar si el usuario existe
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener todos los favoritos y filtrar por usuario
        List<Favoritos> favoritos = favoritosService.listar().stream()
                .filter(f -> f.getUsuario().getIdUsuario().equals(usuarioId))
                .collect(Collectors.toList());

        if (favoritos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<FavoritosDTO> dtos = favoritos.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // POST - Crear un nuevo favorito para un usuario
    @PostMapping("/{usuarioId}")
    public ResponseEntity<FavoritosDTO> crearFavorito(
            @PathVariable Integer usuarioId,
            @RequestBody FavoritosDTO dto) {

        // Validar que el usuarioId del path coincide con el del DTO
        if (!usuarioId.equals(dto.getId_usuario())) {
            return ResponseEntity.badRequest().build();
        }

        // Validar que al menos uno de los IDs (obra o servicio) está presente
        if (dto.getId_obra() == null && dto.getId_servicio() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Verificar que el usuario existe
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Obra obra = null;
        if (dto.getId_obra() != null) {
            obra = obraService.buscarPorId(dto.getId_obra())
                    .orElseThrow(() -> new RuntimeException("Obra no encontrada"));
        }

        Servicio servicio = null;
        if (dto.getId_servicio() != null) {
            servicio = servicioService.buscarPorId(dto.getId_servicio())
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        }

        Favoritos favorito = new Favoritos();
        favorito.setUsuario(usuario);
        favorito.setObra(obra);
        favorito.setServicio(servicio);

        Favoritos guardado = favoritosService.guardar(favorito);
        return ResponseEntity.ok(toDto(guardado));
    }

    // PUT - Actualizar un favorito existente de un usuario
    @PutMapping("/{usuarioId}/{favoritoId}")
    public ResponseEntity<FavoritosDTO> actualizarFavorito(
            @PathVariable Integer usuarioId,
            @PathVariable Integer favoritoId,
            @RequestBody FavoritosDTO dto) {

        // Verificar que el favorito existe y pertenece al usuario
        Favoritos favoritoExistente = favoritosService.buscarPorId(favoritoId)
                .orElseThrow(() -> new RuntimeException("Favorito no encontrado"));

        if (!favoritoExistente.getUsuario().getIdUsuario().equals(usuarioId)) {
            return ResponseEntity.badRequest().build();
        }

        // Validar que al menos uno de los IDs (obra o servicio) está presente
        if (dto.getId_obra() == null && dto.getId_servicio() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Actualizar obra si se proporciona
        if (dto.getId_obra() != null) {
            Obra obra = obraService.buscarPorId(dto.getId_obra())
                    .orElseThrow(() -> new RuntimeException("Obra no encontrada"));
            favoritoExistente.setObra(obra);
        } else {
            favoritoExistente.setObra(null);
        }

        // Actualizar servicio si se proporciona
        if (dto.getId_servicio() != null) {
            Servicio servicio = servicioService.buscarPorId(dto.getId_servicio())
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
            favoritoExistente.setServicio(servicio);
        } else {
            favoritoExistente.setServicio(null);
        }

        Favoritos actualizado = favoritosService.guardar(favoritoExistente);
        return ResponseEntity.ok(toDto(actualizado));
    }

    // DELETE - Eliminar un favorito específico de un usuario
    @DeleteMapping("/{usuarioId}/{favoritoId}")
    public ResponseEntity<Void> eliminarFavorito(
            @PathVariable Integer usuarioId,
            @PathVariable Integer favoritoId) {

        // Verificar que el favorito existe y pertenece al usuario
        Favoritos favorito = favoritosService.buscarPorId(favoritoId)
                .orElseThrow(() -> new RuntimeException("Favorito no encontrado"));

        if (!favorito.getUsuario().getIdUsuario().equals(usuarioId)) {
            return ResponseEntity.badRequest().build();
        }

        favoritosService.eliminar(favoritoId);
        return ResponseEntity.noContent().build();
    }

    // DELETE - Eliminar todos los favoritos de un usuario
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> eliminarTodosFavoritos(@PathVariable Integer usuarioId) {
        // Verificar que el usuario existe
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener y eliminar todos los favoritos del usuario
        List<Favoritos> favoritos = favoritosService.listar().stream()
                .filter(f -> f.getUsuario().getIdUsuario().equals(usuarioId))
                .collect(Collectors.toList());

        favoritos.forEach(f -> favoritosService.eliminar(f.getId_favorito()));

        return ResponseEntity.noContent().build();
    }

    // Método auxiliar para convertir entidad a DTO
    private FavoritosDTO toDto(Favoritos f) {
        return FavoritosDTO.builder()
                .id_favorito(f.getId_favorito())
                .id_usuario(f.getUsuario().getIdUsuario())
                .id_obra(f.getObra() != null ? f.getObra().getIdObra() : null)
                .id_servicio(f.getServicio() != null ? f.getServicio().getIdServicio() : null)
                .build();
    }
}