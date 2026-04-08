package com.example.demo.controller;

import com.example.demo.dto.CapturarOrdenPaypalResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalResponseDTO;
import com.example.demo.service.PaypalPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos/paypal")
@RequiredArgsConstructor
public class PaypalPagoController {

    private final PaypalPagoService paypalPagoService;

    @PostMapping("/crear-orden/{idObra}")
    public ResponseEntity<CrearOrdenPaypalResponseDTO> crearOrden(@PathVariable Integer idObra,
                                                                  @RequestParam Integer compradorId) {
        CrearOrdenPaypalResponseDTO response = paypalPagoService.crearOrdenParaObra(idObra, compradorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/capturar/{paypalOrderId}")
    public ResponseEntity<CapturarOrdenPaypalResponseDTO> capturarOrden(@PathVariable String paypalOrderId) {
        CapturarOrdenPaypalResponseDTO response = paypalPagoService.capturarOrden(paypalOrderId);
        return ResponseEntity.ok(response);
    }
}
