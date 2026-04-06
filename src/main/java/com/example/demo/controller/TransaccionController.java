package com.example.demo.controller;

import com.example.demo.dto.TransaccionResumenDTO;
import com.example.demo.service.TransaccionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/transacciones", "/transacciones"})
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService transaccionService;

    @GetMapping("/compras/{idUsuario}")
    public ResponseEntity<?> obtenerCompras(@PathVariable Integer idUsuario) {
        try {
            List<TransaccionResumenDTO> transacciones = transaccionService.obtenerComprasUsuario(idUsuario);
            return ResponseEntity.ok(transacciones);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/ventas/{idUsuario}")
    public ResponseEntity<?> obtenerVentas(@PathVariable Integer idUsuario) {
        try {
            List<TransaccionResumenDTO> transacciones = transaccionService.obtenerVentasUsuario(idUsuario);
            return ResponseEntity.ok(transacciones);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
