package com.example.demo.controller;

import com.example.demo.dto.ServicioDTO;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.Servicio;
import com.example.demo.service.ServicioService;
import com.example.demo.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/portafolioPersonal")
@RequiredArgsConstructor
public class ServiciosPortafolioPersonalController {

    private final ServicioService servicioService;
    private final UsuarioService usuarioService;

    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<ServicioDTO>> obtenerServiciosPorUsuario(@PathVariable Integer usuarioId) {

        if (usuarioService.buscarPorId(usuarioId).isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<ServicioDTO> servicios = servicioService.buscarPorUsuarioId(usuarioId).stream()
                .map(this::convertirADTO)
                .toList();

        return new ResponseEntity<>(servicios, HttpStatus.OK);
    }

    @PostMapping("/{usuarioId}")
    public ResponseEntity<ServicioDTO> crearServicio(@PathVariable Integer usuarioId,
                                                     @RequestBody ServicioDTO servicioDTO) {

        if (usuarioService.buscarPorId(usuarioId).isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            Servicio creado = servicioService.crearServicioParaUsuario(usuarioId, servicioDTO);
            return new ResponseEntity<>(convertirADTO(creado), HttpStatus.CREATED);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{usuarioId}/{idServicio}")
    public ResponseEntity<ServicioDTO> actualizarServicio(@PathVariable Integer usuarioId,
                                                          @PathVariable Integer idServicio,
                                                          @RequestBody ServicioDTO servicioActualizado) {

        try {
            Servicio actualizado = servicioService.actualizarServicioDeUsuario(usuarioId, idServicio, servicioActualizado);
            return new ResponseEntity<>(convertirADTO(actualizado), HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/{usuarioId}/{idServicio}")
    public ResponseEntity<Void> eliminarServicio(@PathVariable Integer usuarioId,
                                                 @PathVariable Integer idServicio) {

        try {
            servicioService.eliminarServicioDeUsuario(usuarioId, idServicio);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    private ServicioDTO convertirADTO(Servicio s) {

        Integer idCategoria = null;
        String nombreCategoria = null;


        if (s.getCategoriasServicios() != null && !s.getCategoriasServicios().isEmpty()) {
            CategoriaServicios cs = s.getCategoriasServicios().iterator().next();
            if (cs.getCategoria() != null) {
                idCategoria = cs.getCategoria().getIdCategoria();
                nombreCategoria = cs.getCategoria().getNombreCategoria();
            }
        }
        return ServicioDTO.builder()
                .idServicio(s.getIdServicio())
                .titulo(s.getTitulo())
                .descripcion(s.getDescripcion())
                .tipoContacto(s.getTipoContacto())
                .contacto(s.getContacto())
                .tecnicas(s.getTecnicas())
                .precioMin(s.getPrecioMin())
                .precioMax(s.getPrecioMax())
                .idUsuario(
                        s.getUsuario() != null ? s.getUsuario().getIdUsuario() : null
                )
                .nombreUsuario(
                        s.getUsuario() != null ? s.getUsuario().getUsuario() : "Desconocido"
                )
                .fotoPerfilAutor(
                        s.getUsuario() != null ? s.getUsuario().getFotoPerfil() : null
                )
                .idCategoria(idCategoria)
                .categoria(nombreCategoria)
                .build();
    }
}
