package com.example.demo.controller;

import com.example.demo.dto.ObraDTO;
import com.example.demo.model.CategoriaObras;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.service.ObraService;
import com.example.demo.service.UsuarioService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<ObraDTO>> obtenerObrasPorUsuario(@PathVariable Integer usuarioId) {

        usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));

        List<Obra> obras = obraService.buscarPorUsuarioId(usuarioId);

        if (obras.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<ObraDTO> dtos = obras.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{usuarioId}")
    public ResponseEntity<ObraDTO> crearObraParaUsuario(
            @PathVariable Integer usuarioId,
            @RequestBody ObraDTO obraDTO) {

        // Verifica que el usuario existe antes de intentar guardar la obra
        usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));
        Obra guardada = obraService.guardarObraConCategoria(usuarioId, obraDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(convertirADTO(guardada));
    }

    @PutMapping("/{usuarioId}/{obraId}")
    public ResponseEntity<ObraDTO> actualizarObraDeUsuario(
            @PathVariable Integer usuarioId,
            @PathVariable Integer obraId,
            @RequestBody ObraDTO obraDTO) {

        Obra obraExistente = obraService.buscarPorId(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada con ID: " + obraId));


        if (!obraExistente.getUsuario().getIdUsuario().equals(usuarioId)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }


        obraExistente.setTitulo(obraDTO.getTitulo());
        obraExistente.setDescripcion(obraDTO.getDescripcion());
        obraExistente.setEstado(obraDTO.getEstado());
        obraExistente.setPrecio(obraDTO.getPrecio());
        obraExistente.setImagen1(obraDTO.getImagen1());
        obraExistente.setImagen2(obraDTO.getImagen2());
        obraExistente.setImagen3(obraDTO.getImagen3());
        obraExistente.setTecnicas(obraDTO.getTecnicas());
        obraExistente.setMedidas(obraDTO.getMedidas());

        Obra actualizada = obraService.guardar(obraExistente);
        return ResponseEntity.ok(convertirADTO(actualizada));
    }

    @DeleteMapping("/{usuarioId}/{obraId}")
    public ResponseEntity<Void> eliminarObraDeUsuario(
            @PathVariable Integer usuarioId,
            @PathVariable Integer obraId) {

        Obra obra = obraService.buscarPorId(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada con ID: " + obraId));

        if (!obra.getUsuario().getIdUsuario().equals(usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        obraService.eliminar(obraId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> eliminarTodasLasObrasDeUsuario(@PathVariable Integer usuarioId) {
        // Verificar que el usuario existe
        usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));

        obraService.eliminarPorUsuarioId(usuarioId);

        return ResponseEntity.noContent().build();
    }

    private ObraDTO convertirADTO(Obra o) {
        Integer idUsuario = (o.getUsuario() != null) ? o.getUsuario().getIdUsuario() : null;

        String nombreAutor = (o.getUsuario() != null) ? o.getUsuario().getUsuario() : "Desconocido";

        String fotoPerfilAutor = (o.getUsuario() != null) ? o.getUsuario().getFotoPerfil() : null;

        Integer idCategoria = null;
        String nombreCategoria = "Sin Categoría";

        if (o.getCategoriaObras() != null && !o.getCategoriaObras().isEmpty()) {
            CategoriaObras co = o.getCategoriaObras().iterator().next();
            if (co.getCategoria() != null) {
                idCategoria = co.getCategoria().getIdCategoria();
                nombreCategoria = co.getCategoria().getNombreCategoria();
            }
        }

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
                // Se incluyen los nuevos campos poblados
                .idCategoria(idCategoria)
                .nombreAutor(nombreAutor)
                .nombreCategoria(nombreCategoria)
                .fotoPerfilAutor(fotoPerfilAutor)
                .build();
    }
}