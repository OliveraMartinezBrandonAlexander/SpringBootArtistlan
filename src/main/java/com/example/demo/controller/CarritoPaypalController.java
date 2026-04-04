package com.example.demo.controller;

import com.example.demo.dto.CapturarOrdenPaypalCarritoResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalCarritoResponseDTO;
import com.example.demo.service.PaypalCarritoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito/paypal")
@RequiredArgsConstructor
public class CarritoPaypalController {

    private final PaypalCarritoService paypalCarritoService;

    @PostMapping("/crear-orden/{idUsuario}")
    public ResponseEntity<?> crearOrdenCarrito(@PathVariable Integer idUsuario) {
        try {
            CrearOrdenPaypalCarritoResponseDTO response = paypalCarritoService.crearOrdenParaCarrito(idUsuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/capturar/{paypalOrderId}")
    public ResponseEntity<?> capturarOrdenCarrito(@PathVariable String paypalOrderId) {
        try {
            CapturarOrdenPaypalCarritoResponseDTO response = paypalCarritoService.capturarOrdenCarrito(paypalOrderId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
