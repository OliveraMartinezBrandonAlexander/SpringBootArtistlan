package com.example.demo.controller;

import com.example.demo.dto.ActualizarImagenObraRequestDTO;
import com.example.demo.dto.ObraDTO;
import com.example.demo.model.Obra;
import com.example.demo.service.ObraService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/obras")
@AllArgsConstructor
public class ObraController {

    private final ObraService service;

    // GET todos
    @GetMapping
    public ResponseEntity<List<ObraDTO>> obtenerTodas() {
        List<Obra> obras = service.listar();
        if (obras.isEmpty()) return ResponseEntity.noContent().build();

        List<ObraDTO> dtos = obras.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // GET por id
    @GetMapping("/{id}")
    public ResponseEntity<ObraDTO> obtenerPorId(@PathVariable Integer id) {
        return service.buscarPorId(id)
                .map(o -> ResponseEntity.ok(convertirADTO(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST
    @PostMapping
    public ResponseEntity<ObraDTO> subirObra(@RequestBody Obra obra) {
        Obra guardada = service.guardar(obra);
        return ResponseEntity.ok(convertirADTO(guardada));
    }

    // PUT por id
    @PutMapping("/{id}")
    public ResponseEntity<ObraDTO> actualizarPorId(@PathVariable Integer id, @RequestBody Obra nuevosDatos) {
        return service.actualizarObra(id, nuevosDatos)
                .map(o -> ResponseEntity.ok(convertirADTO(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE por id
    @DeleteMapping( "/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Integer id) {
        boolean eliminado = service.eliminar(id);
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/imagen1")
    public ResponseEntity<Obra> actualizarImagen1(
            @PathVariable Integer id,
            @RequestBody ActualizarImagenObraRequestDTO requestDTO
            ) {
        return service.actualizarImagen1(id, requestDTO.getImagen1())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Conversión a DTO
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