package com.example.demo.controller;

import com.example.demo.dto.ObraDTO;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.service.ObraService;
import com.example.demo.service.UsuarioService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/obrasDeUsuario")
@AllArgsConstructor
public class ObrasDeUsuarioController {

    private final ObraService obraService;
    private final UsuarioService usuarioService;

    // GET - Obtener todas las obras de un usuario específico
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<ObraDTO>> obtenerObrasPorUsuario(@PathVariable Integer usuarioId) {
        // Verificar si el usuario existe
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener todas las obras y filtrar por usuario
        List<Obra> obras = obraService.listar().stream()
                .filter(o -> o.getUsuario().getIdUsuario().equals(usuarioId))
                .collect(Collectors.toList());

        if (obras.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<ObraDTO> dtos = obras.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // POST - Crear una nueva obra para un usuario específico
    @PostMapping("/{usuarioId}")
    public ResponseEntity<ObraDTO> crearObraParaUsuario(
            @PathVariable Integer usuarioId,
            @RequestBody Obra obra) {

        // Verificar que el usuario existe
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Asignar el usuario a la obra
        obra.setUsuario(usuario);
        Obra guardada = obraService.guardar(obra);

        return ResponseEntity.ok(convertirADTO(guardada));
    }

    // PUT - Actualizar una obra específica de un usuario
    @PutMapping("/{usuarioId}/{obraId}")
    public ResponseEntity<ObraDTO> actualizarObraDeUsuario(
            @PathVariable Integer usuarioId,
            @PathVariable Integer obraId,
            @RequestBody Obra obraActualizada) {

        // Verificar que la obra existe y pertenece al usuario
        Obra obraExistente = obraService.buscarPorId(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        if (!obraExistente.getUsuario().getIdUsuario().equals(usuarioId)) {
            return ResponseEntity.badRequest().build();
        }

        // Actualizar los campos de la obra
        obraExistente.setTitulo(obraActualizada.getTitulo());
        obraExistente.setDescripcion(obraActualizada.getDescripcion());
        obraExistente.setEstado(obraActualizada.getEstado());
        obraExistente.setPrecio(obraActualizada.getPrecio());
        obraExistente.setImagen1(obraActualizada.getImagen1());
        obraExistente.setImagen2(obraActualizada.getImagen2());
        obraExistente.setImagen3(obraActualizada.getImagen3());
        obraExistente.setTecnicas(obraActualizada.getTecnicas());
        obraExistente.setMedidas(obraActualizada.getMedidas());
        obraExistente.setLikes(obraActualizada.getLikes());

        Obra actualizada = obraService.guardar(obraExistente);
        return ResponseEntity.ok(convertirADTO(actualizada));
    }

    // DELETE - Eliminar una obra específica de un usuario
    @DeleteMapping("/{usuarioId}/{obraId}")
    public ResponseEntity<Void> eliminarObraDeUsuario(
            @PathVariable Integer usuarioId,
            @PathVariable Integer obraId) {

        // Verificar que la obra existe y pertenece al usuario
        Obra obra = obraService.buscarPorId(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        if (!obra.getUsuario().getIdUsuario().equals(usuarioId)) {
            return ResponseEntity.badRequest().build();
        }

        obraService.eliminar(obraId);
        return ResponseEntity.noContent().build();
    }

    // DELETE - Eliminar todas las obras de un usuario
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> eliminarTodasLasObrasDeUsuario(@PathVariable Integer usuarioId) {
        // Verificar que el usuario existe
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener y eliminar todas las obras del usuario
        List<Obra> obras = obraService.listar().stream()
                .filter(o -> o.getUsuario().getIdUsuario().equals(usuarioId))
                .collect(Collectors.toList());

        obras.forEach(o -> obraService.eliminar(o.getIdObra()));

        return ResponseEntity.noContent().build();
    }

    // Método auxiliar para convertir entidad a DTO
    private ObraDTO convertirADTO(Obra o) {
        Integer idUsuario = (o.getUsuario() != null) ? o.getUsuario().getIdUsuario() : null;
        return ObraDTO.builder()
                .idObra(o.getIdObra())
                .titulo(o.getTitulo())
                .descripcion(o.getDescripcion())
                .estado(o.getEstado())
                .precio(o.getPrecio())
                .imagen1(o.getImagen1())
                .imagen2(o.getImagen2())
                .imagen3(o.getImagen3())
                .tecnicas(o.getTecnicas())
                .medidas(o.getMedidas())
                .likes(o.getLikes())
                .idUsuario(idUsuario)
                .build();
    }
}