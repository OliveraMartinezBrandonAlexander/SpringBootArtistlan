package com.example.demo.controller;

import com.example.demo.dto.ActualizarImagenObraRequestDTO;
import com.example.demo.dto.ObraDTO;
import com.example.demo.model.CategoriaObras;
import com.example.demo.model.Obra;
import com.example.demo.service.FavoritosService;
import com.example.demo.service.ObraService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/obras")
@AllArgsConstructor
public class ObraController {

    private final ObraService service;
    private final FavoritosService favoritosService;

    @GetMapping
    public ResponseEntity<List<ObraDTO>> obtenerTodas(@RequestParam(required = false) Integer usuarioId) {
        List<Obra> obras = service.listar();
        if (obras.isEmpty()) return ResponseEntity.noContent().build();

        List<ObraDTO> dtos = obras.stream()
                .map(o -> convertirADTO(o, usuarioId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObraDTO> obtenerPorId(@PathVariable Integer id,
                                                @RequestParam(required = false) Integer usuarioId) {
        return service.buscarPorId(id)
                .map(o -> ResponseEntity.ok(convertirADTO(o, usuarioId)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ObraDTO> subirObra(@RequestBody Obra obra,
                                             @RequestParam(required = false) Integer usuarioId) {
        Obra guardada = service.guardar(obra);
        return ResponseEntity.ok(convertirADTO(guardada, usuarioId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObraDTO> actualizarPorId(@PathVariable Integer id,
                                                   @RequestBody Obra nuevosDatos,
                                                   @RequestParam(required = false) Integer usuarioId) {
        return service.actualizarObra(id, nuevosDatos)
                .map(o -> ResponseEntity.ok(convertirADTO(o, usuarioId)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Integer id) {
        boolean eliminado = service.eliminar(id);
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/imagen1")
    public ResponseEntity<Obra> actualizarImagen1(@PathVariable Integer id,
                                                  @RequestBody ActualizarImagenObraRequestDTO requestDTO) {
        return service.actualizarImagen1(id, requestDTO.getImagen1())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private ObraDTO convertirADTO(Obra o, Integer usuarioId) {
        Integer idUsuario = Optional.ofNullable(o.getUsuario()).map(u -> u.getIdUsuario()).orElse(null);
        String nombreAutor = Optional.ofNullable(o.getUsuario()).map(u -> u.getUsuario()).orElse("Desconocido");
        String fotoPerfilAutor = Optional.ofNullable(o.getUsuario()).map(u -> u.getFotoPerfil()).orElse(null);

        Integer idCategoria = null;
        String nombreCategoria = "Sin Categoría";

        if (o.getCategoriaObras() != null && !o.getCategoriaObras().isEmpty()) {
            List<CategoriaObras> listaCategorias = new ArrayList<>(o.getCategoriaObras());
            CategoriaObras co = listaCategorias.get(0);
            if (co.getCategoria() != null) {
                idCategoria = co.getCategoria().getIdCategoria();
                nombreCategoria = co.getCategoria().getNombreCategoria();
            }
        }
        int likes = favoritosService.likesPorObra(o.getIdObra().longValue());
        boolean esFavorito = favoritosService.esObraFavorita(usuarioId, o.getIdObra());

        String estado = normalizarEstado(o.getEstado());
        boolean propia = usuarioId != null && usuarioId.equals(idUsuario);

        return ObraDTO.builder()
                .idObra(o.getIdObra())
                .titulo(o.getTitulo())
                .descripcion(o.getDescripcion())
                .estado(estadoParaMostrar(o.getEstado()))
                .precio(o.getPrecio())
                .imagen1(o.getImagen1())
                .imagen2(o.getImagen2())
                .imagen3(o.getImagen3())
                .tecnicas(o.getTecnicas())
                .medidas(o.getMedidas())
                .confirmacionAutoria(o.getConfirmacionAutoria())
                .likes(likes)
                .esFavorito(esFavorito)
                .idUsuario(idUsuario)
                .idCategoria(idCategoria)
                .nombreAutor(nombreAutor)
                .nombreCategoria(nombreCategoria)
                .fotoPerfilAutor(fotoPerfilAutor)
                .editable(!"RESERVADA".equals(estado) && !"VENDIDA".equals(estado) && propia)
                .eliminable(!"RESERVADA".equals(estado) && !"VENDIDA".equals(estado) && propia)
                .puedeSolicitarCompra("EN_VENTA".equals(estado) && !propia)
                .build();
    }

    private String normalizarEstado(String estado) {
        if (estado == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(estado, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String estadoParaMostrar(String estadoOriginal) {
        String estado = normalizarEstado(estadoOriginal);
        return switch (estado) {
            case "EN_VENTA" -> "En venta";
            case "EN_EXHIBICION" -> "En exhibici\u00F3n";
            case "RESERVADA" -> "Reservada";
            case "VENDIDA" -> "Vendida";
            default -> estadoOriginal;
        };
    }
}
