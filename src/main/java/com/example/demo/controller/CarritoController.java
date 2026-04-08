package com.example.demo.controller;

import com.example.demo.dto.CarritoDTO;
import com.example.demo.dto.CarritoContactoDTO;
import com.example.demo.dto.CarritoRequestDTO;
import com.example.demo.dto.CarritoTotalDTO;
import com.example.demo.exception.BusinessException;
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

    @GetMapping("/{idUsuario}/obras")
    public ResponseEntity<List<CarritoDTO>> listarObrasCarrito(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(carritoService.listarObrasEnCarrito(idUsuario));
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

    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<Void> limpiarCarrito(@PathVariable Integer idUsuario,
                                               @RequestParam(name = "confirmar", defaultValue = "false") boolean confirmar) {
        if (!confirmar) {
            throw new BusinessException("Operacion sensible: confirma con ?confirmar=true para limpiar todo el carrito.");
        }
        carritoService.limpiarCarritoUsuario(idUsuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{idUsuario}/total")
    public ResponseEntity<CarritoTotalDTO> obtenerTotal(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(carritoService.obtenerTotal(idUsuario));
    }

    @GetMapping("/{idUsuario}/{idObra}/contacto")
    public ResponseEntity<CarritoContactoDTO> obtenerContactoVendedor(@PathVariable Integer idUsuario,
                                                                      @PathVariable Integer idObra) {
        return ResponseEntity.ok(carritoService.obtenerContactoVendedor(idUsuario, idObra));
    }

    @PostMapping("/reservas/expirar")
    public ResponseEntity<Integer> expirarReservas() {
        return ResponseEntity.ok(carritoService.limpiarReservasVencidas());
    }
}
