package com.example.demo.controller;

import com.example.demo.dto.TransaccionDetalleDTO;
import com.example.demo.dto.TransaccionResumenDTO;
import com.example.demo.service.TransaccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/transacciones", "/transacciones"})
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService transaccionService;

    @GetMapping("/compras/{idUsuario}")
    public ResponseEntity<List<TransaccionResumenDTO>> obtenerCompras(@PathVariable Integer idUsuario) {
        List<TransaccionResumenDTO> transacciones = transaccionService.obtenerComprasUsuario(idUsuario);
        return ResponseEntity.ok(transacciones);
    }

    @GetMapping("/ventas/{idUsuario}")
    public ResponseEntity<List<TransaccionResumenDTO>> obtenerVentas(@PathVariable Integer idUsuario) {
        List<TransaccionResumenDTO> transacciones = transaccionService.obtenerVentasUsuario(idUsuario);
        return ResponseEntity.ok(transacciones);
    }

    @GetMapping("/{idUsuario}/detalle")
    public ResponseEntity<TransaccionDetalleDTO> obtenerDetalle(@PathVariable Integer idUsuario,
                                                                @RequestParam String tipoOrigen,
                                                                @RequestParam Integer idTransaccion) {
        TransaccionDetalleDTO detalle = transaccionService.obtenerDetalleTransaccion(idUsuario, tipoOrigen, idTransaccion);
        return ResponseEntity.ok(detalle);
    }
}
