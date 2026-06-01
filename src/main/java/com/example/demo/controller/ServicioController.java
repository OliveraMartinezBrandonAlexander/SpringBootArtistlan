package com.example.demo.controller;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dto.ServicioDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.Servicio;
import com.example.demo.service.FavoritosService;
import com.example.demo.service.ServicioService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@AllArgsConstructor
@Slf4j
public class ServicioController {

    private final ServicioService service;
    private final FavoritosService favoritosService;

    @GetMapping
    public ResponseEntity<List<ServicioDTO>> obtenerTodos(@RequestParam(required = false) Integer usuarioId) {
        Integer usuarioConsulta = SecurityUtils.obtenerIdUsuarioAutenticadoSiExiste();
        List<Servicio> servicios = service.listarServiciosPublicosVisibles();
        if (servicios.isEmpty()) return ResponseEntity.noContent().build();

        List<ServicioDTO> dtos = servicios.stream()
                .map(s -> convertirADTO(s, usuarioConsulta))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/paginado")
    public ResponseEntity<PageResponseDTO<ServicioDTO>> obtenerTodosPaginado(
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Integer idCategoria,
            @PageableDefault(size = 10, sort = "idServicio") Pageable pageable
    ) {
        Integer usuarioConsulta = SecurityUtils.obtenerIdUsuarioAutenticadoSiExiste();
        String query = StringUtils.hasText(q) ? q.trim() : null;
        Integer categoriaId = idCategoria;
        String categoriaNombre = categoriaId != null
                ? null
                : (StringUtils.hasText(categoria) ? categoria.trim() : null);

        Page<ServicioDTO> page = service.listarServiciosPublicosVisiblesPaginado(query, categoriaNombre, categoriaId, pageable)
                .map(servicio -> convertirADTO(servicio, usuarioConsulta));

        return ResponseEntity.ok(PageResponseDTO.fromPage(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioDTO> obtenerPorId(@PathVariable Integer id,
                                                    @RequestParam(required = false) Integer usuarioId) {
        Integer usuarioConsulta = SecurityUtils.obtenerIdUsuarioAutenticadoSiExiste();
        if (id == null || id <= 0) {
            log.info("ServicioCrudBackendDebug GET detalle 400 idServicio={} usuarioId={}", id, usuarioId);
            return ResponseEntity.badRequest().build();
        }
        return service.buscarDetalleVisibleOPropioPorId(id, usuarioConsulta)
                .map(s -> ResponseEntity.ok(convertirADTO(s, usuarioConsulta)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ServicioDTO> crear(@RequestBody ServicioDTO dto,
                                             @RequestParam(required = false) Integer usuarioId) {
        Integer usuarioConsulta = SecurityUtils.obtenerIdUsuarioAutenticado();
        Servicio guardado = service.crearServicioParaUsuario(dto.getIdUsuario(), dto);
        return ResponseEntity.ok(convertirADTO(guardado, usuarioConsulta));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioDTO> actualizarPorId(@PathVariable Integer id,
                                                       @RequestBody ServicioDTO nuevosDatos,
                                                       @RequestParam(required = false) Integer usuarioId) {
        Integer usuarioConsulta = SecurityUtils.obtenerIdUsuarioAutenticado();
        return service.actualizarServicio(id, nuevosDatos)
                .map(s -> ResponseEntity.ok(convertirADTO(s, usuarioConsulta)))
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
                .tipoContacto(s.getTipoContacto())
                .contacto(s.getContacto())
                .tecnicas(s.getTecnicas())
                .precioMin(s.getPrecioMin())
                .precioMax(s.getPrecioMax())
                .fechaPublicacion(s.getFechaPublicacion())
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
