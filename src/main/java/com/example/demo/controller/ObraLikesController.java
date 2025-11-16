package com.example.demo.controller;

import com.example.demo.dto.ObraDTO;
import com.example.demo.model.Obra;
import com.example.demo.service.ObraService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/obrasLikes")
@AllArgsConstructor
public class ObraLikesController {

    private final ObraService obraService;

    // GET - Obtener los likes de una obra específica
    @GetMapping("/{obraId}")
    public ResponseEntity<Integer> obtenerLikesDeObra(@PathVariable Integer obraId) {
        Obra obra = obraService.buscarPorId(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        return ResponseEntity.ok(obra.getLikes());
    }

    // POST - Incrementar los likes de una obra (dar like)
    @PostMapping("/{obraId}")
    public ResponseEntity<ObraDTO> darLike(@PathVariable Integer obraId) {
        Obra obra = obraService.buscarPorId(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        // Incrementar likes
        obra.setLikes(obra.getLikes() + 1);
        Obra actualizada = obraService.guardar(obra);

        return ResponseEntity.ok(convertirADTO(actualizada));
    }

    // PUT - Actualizar el número específico de likes de una obra
    @PutMapping("/{obraId}")
    public ResponseEntity<ObraDTO> actualizarLikes(
            @PathVariable Integer obraId,
            @RequestBody Integer nuevosLikes) {

        Obra obra = obraService.buscarPorId(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        // Validar que los likes no sean negativos
        if (nuevosLikes < 0) {
            return ResponseEntity.badRequest().build();
        }

        obra.setLikes(nuevosLikes);
        Obra actualizada = obraService.guardar(obra);

        return ResponseEntity.ok(convertirADTO(actualizada));
    }

    // DELETE - Decrementar likes de una obra (quitar like)
    @DeleteMapping("/{obraId}")
    public ResponseEntity<ObraDTO> quitarLike(@PathVariable Integer obraId) {
        Obra obra = obraService.buscarPorId(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        // Decrementar likes, pero no permitir valores negativos
        int nuevosLikes = Math.max(0, obra.getLikes() - 1);
        obra.setLikes(nuevosLikes);
        Obra actualizada = obraService.guardar(obra);

        return ResponseEntity.ok(convertirADTO(actualizada));
    }

    // DELETE - Reiniciar likes a 0 de una obra específica
    @DeleteMapping("/{obraId}/reiniciar")
    public ResponseEntity<ObraDTO> reiniciarLikes(@PathVariable Integer obraId) {
        Obra obra = obraService.buscarPorId(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        obra.setLikes(0);
        Obra actualizada = obraService.guardar(obra);

        return ResponseEntity.ok(convertirADTO(actualizada));
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