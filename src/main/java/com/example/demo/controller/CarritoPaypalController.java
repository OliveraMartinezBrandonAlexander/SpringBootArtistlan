package com.example.demo.controller;

import com.example.demo.dto.CapturarOrdenPaypalCarritoResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalCarritoResponseDTO;
import com.example.demo.service.PaypalCarritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carrito/paypal")
@RequiredArgsConstructor
public class CarritoPaypalController {

    private final PaypalCarritoService paypalCarritoService;

    @PostMapping("/crear-orden/{idUsuario}")
    public ResponseEntity<CrearOrdenPaypalCarritoResponseDTO> crearOrdenCarrito(@PathVariable Integer idUsuario) {
        CrearOrdenPaypalCarritoResponseDTO response = paypalCarritoService.crearOrdenParaCarrito(idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/capturar/{paypalOrderId}")
    public ResponseEntity<CapturarOrdenPaypalCarritoResponseDTO> capturarOrdenCarrito(@PathVariable String paypalOrderId) {
        CapturarOrdenPaypalCarritoResponseDTO response = paypalCarritoService.capturarOrdenCarrito(paypalOrderId);
        return ResponseEntity.ok(response);
    }
}
