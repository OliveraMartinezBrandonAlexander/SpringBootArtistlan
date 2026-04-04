package com.example.demo.controller;

import com.example.demo.dto.CapturarOrdenPaypalResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalResponseDTO;
import com.example.demo.service.PaypalPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos/paypal")
@RequiredArgsConstructor
public class PaypalPagoController {

    private final PaypalPagoService paypalPagoService;

    @PostMapping("/crear-orden/{idObra}")
    public ResponseEntity<?> crearOrden(@PathVariable Integer idObra,
                                        @RequestParam Integer compradorId) {
        try {
            CrearOrdenPaypalResponseDTO response = paypalPagoService.crearOrdenParaObra(idObra, compradorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/capturar/{paypalOrderId}")
    public ResponseEntity<?> capturarOrden(@PathVariable String paypalOrderId) {
        try {
            CapturarOrdenPaypalResponseDTO response = paypalPagoService.capturarOrden(paypalOrderId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
