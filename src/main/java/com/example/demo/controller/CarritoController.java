package com.example.demo.controller;

import com.example.demo.dto.CarritoDTO;
import com.example.demo.dto.CarritoRequestDTO;
import com.example.demo.service.CarritoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    @PostMapping("/agregar")
    public ResponseEntity<?> agregarAlCarrito(@RequestBody CarritoRequestDTO request) {
        try {
            CarritoDTO carrito = carritoService.agregarAlCarrito(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(carrito);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> obtenerCarritoUsuario(@PathVariable Integer idUsuario) {
        try {
            List<CarritoDTO> carrito = carritoService.obtenerCarritoUsuario(idUsuario);
            if (carrito.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(carrito);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/eliminar/{idUsuario}/{idObra}")
    public ResponseEntity<?> eliminarDelCarrito(@PathVariable Integer idUsuario,
                                                @PathVariable Integer idObra) {
        try {
            carritoService.eliminarDelCarrito(CarritoRequestDTO.builder()
                    .idUsuario(idUsuario)
                    .idObra(idObra)
                    .build());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
