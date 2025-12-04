package com.example.demo.controller;

import com.example.demo.dto.ServicioDTO;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.Servicio;
import com.example.demo.service.ServicioService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servicios")
@AllArgsConstructor
public class ServicioController {

    private final ServicioService service;

    // GET todos
    @GetMapping
    public ResponseEntity<List<ServicioDTO>> obtenerTodos() {
        List<Servicio> servicios = service.todosServicios();
        if (servicios.isEmpty()) return ResponseEntity.noContent().build();

        List<ServicioDTO> dtos = servicios.stream()
                .map(this::convertirADTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // GET por id
    @GetMapping("/{id}")
    public ResponseEntity<ServicioDTO> obtenerPorId(@PathVariable Integer id) {
        return service.buscarPorId(id)
                .map(s -> ResponseEntity.ok(convertirADTO(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST
    @PostMapping
    public ResponseEntity<ServicioDTO> crear(@RequestBody ServicioDTO dto) {
        Servicio guardado = service.crearServicioParaUsuario(dto.getIdUsuario(), dto);
        return ResponseEntity.ok(convertirADTO(guardado));
    }

    // PUT por id
    @PutMapping("/{id}")
    public ResponseEntity<ServicioDTO> actualizarPorId(@PathVariable Integer id, @RequestBody Servicio nuevosDatos) {
        return service.actualizarServicio(id, nuevosDatos)
                .map(s -> ResponseEntity.ok(convertirADTO(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE por id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Integer id) {
        boolean eliminado = service.eliminarServicio(id);
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // Conversión a DTO
    private ServicioDTO convertirADTO(Servicio s) {

        ServicioDTO.ServicioDTOBuilder builder = ServicioDTO.builder()
                .idServicio(s.getIdServicio())
                .titulo(s.getTitulo())
                .descripcion(s.getDescripcion())
                .contacto(s.getContacto())
                .tecnicas(s.getTecnicas())
                .idUsuario(s.getUsuario() != null ? s.getUsuario().getIdUsuario() : null)
                .nombreUsuario(s.getUsuario() != null ? s.getUsuario().getUsuario() : "Desconocido");

        if (s.getCategoriasServicios() != null && !s.getCategoriasServicios().isEmpty()) {
            s.getCategoriasServicios().size();
            CategoriaServicios cs = s.getCategoriasServicios().iterator().next();
            if (cs.getCategoria() != null) {
                builder.idCategoria(cs.getCategoria().getIdCategoria());
                builder.categoria(cs.getCategoria().getNombreCategoria());
            }
        }

        return builder.build();
    }
}