package com.example.demo.controller;

import com.example.demo.dto.ServicioDTO;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.Servicio;
import com.example.demo.service.FavoritosService;
import com.example.demo.service.ServicioService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@AllArgsConstructor
public class ServicioController {

    private final ServicioService service;
    private final FavoritosService favoritosService;

    @GetMapping
    public ResponseEntity<List<ServicioDTO>> obtenerTodos(@RequestParam(required = false) Integer usuarioId) {
        List<Servicio> servicios = service.todosServicios();
        if (servicios.isEmpty()) return ResponseEntity.noContent().build();

        List<ServicioDTO> dtos = servicios.stream()
                .map(s -> convertirADTO(s, usuarioId))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioDTO> obtenerPorId(@PathVariable Integer id,
                                                    @RequestParam(required = false) Integer usuarioId) {
        return service.buscarPorId(id)
                .map(s -> ResponseEntity.ok(convertirADTO(s, usuarioId)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ServicioDTO> crear(@RequestBody ServicioDTO dto,
                                             @RequestParam(required = false) Integer usuarioId) {
        Servicio guardado = service.crearServicioParaUsuario(dto.getIdUsuario(), dto);
        return ResponseEntity.ok(convertirADTO(guardado, usuarioId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioDTO> actualizarPorId(@PathVariable Integer id,
                                                       @RequestBody Servicio nuevosDatos,
                                                       @RequestParam(required = false) Integer usuarioId) {
        return service.actualizarServicio(id, nuevosDatos)
                .map(s -> ResponseEntity.ok(convertirADTO(s, usuarioId)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Integer id) {
        boolean eliminado = service.eliminarServicio(id);
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private ServicioDTO convertirADTO(Servicio s, Integer usuarioId) {

        ServicioDTO.ServicioDTOBuilder builder = ServicioDTO.builder()
                .idServicio(s.getIdServicio())
                .titulo(s.getTitulo())
                .descripcion(s.getDescripcion())
                .contacto(s.getContacto())
                .tecnicas(s.getTecnicas())
                .likes(favoritosService.likesPorServicio(s.getIdServicio().longValue()))
                .esFavorito(favoritosService.esServicioFavorito(usuarioId, s.getIdServicio()))
                .idUsuario(s.getUsuario() != null ? s.getUsuario().getIdUsuario() : null)
                .nombreUsuario(s.getUsuario() != null ? s.getUsuario().getUsuario() : "Desconocido")
                .fotoPerfilAutor(s.getUsuario() != null ? s.getUsuario().getFotoPerfil() : null);

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