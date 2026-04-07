package com.example.demo.controller;

import com.example.demo.dto.CarritoDTO;
import com.example.demo.dto.CarritoRequestDTO;
import com.example.demo.dto.CarritoTotalDTO;
import com.example.demo.service.CarritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping("/{idUsuario}")
    public ResponseEntity<List<CarritoDTO>> obtenerCarritoUsuario(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(carritoService.obtenerCarritoUsuario(idUsuario));
    }

    @DeleteMapping("/eliminar/{idUsuario}/{idObra}")
    public ResponseEntity<Void> eliminarDelCarrito(@PathVariable Integer idUsuario,
                                                   @PathVariable Integer idObra) {
        carritoService.eliminarDelCarrito(CarritoRequestDTO.builder()
                .idUsuario(idUsuario)
                .idObra(idObra)
                .build());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{idUsuario}/total")
    public ResponseEntity<CarritoTotalDTO> obtenerTotal(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(carritoService.obtenerTotal(idUsuario));
    }

    @PostMapping("/reservas/expirar")
    public ResponseEntity<Integer> expirarReservas() {
        return ResponseEntity.ok(carritoService.limpiarReservasVencidas());
    }
}
