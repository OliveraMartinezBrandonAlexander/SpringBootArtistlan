package com.example.demo.controller;

import com.example.demo.model.Servicio;
import com.example.demo.model.Usuario;
import com.example.demo.service.ServicioService;
import com.example.demo.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/portafolioPersonal")
@RequiredArgsConstructor
public class ServiciosPortafolioPersonalController {

    private final ServicioService servicioService;
    private final UsuarioService usuarioService; //relacionar el usuario y el servicio

    // GET: Obtener todos los servicios de un usuario
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<Servicio>> obtenerServiciosPorUsuario(@PathVariable Integer usuarioId) {
        Optional<Usuario> usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        List<Servicio> servicios = servicioService.todosServicios().stream()
                .filter(s -> s.getUsuario().getIdUsuario().equals(usuarioId))
                .toList();
        return new ResponseEntity<>(servicios, HttpStatus.OK);
    }

    // POST: Crear un nuevo servicio para un usuario
    @PostMapping("/{usuarioId}")
    public ResponseEntity<Servicio> crearServicio(@PathVariable Integer usuarioId,
                                                  @RequestBody Servicio servicio) {
        Optional<Usuario> usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        servicio.setUsuario(usuario.get());
        Servicio creado = servicioService.guardarServicio(servicio);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    // PUT: Actualizar un servicio existente
    @PutMapping("/{idServicio}")
    public ResponseEntity<Servicio> actualizarServicio(@PathVariable Integer idServicio,
                                                       @RequestBody Servicio servicioActualizado) {
        Optional<Servicio> actualizado = servicioService.actualizarServicio(idServicio, servicioActualizado);
        return actualizado
                .map(s -> new ResponseEntity<>(s, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // DELETE: Eliminar un servicio
    @DeleteMapping("/{idServicio}")
    public ResponseEntity<Void> eliminarServicio(@PathVariable Integer idServicio) {
        boolean eliminado = servicioService.eliminarServicio(idServicio);
        return eliminado
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
