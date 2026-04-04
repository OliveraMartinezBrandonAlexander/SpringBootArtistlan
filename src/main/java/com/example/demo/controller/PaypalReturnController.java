package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/paypal")
public class PaypalReturnController {

    @GetMapping("/return")
    public ResponseEntity<Map<String, Object>> paypalReturn(@RequestParam("token") String token,
                                                            @RequestParam(value = "PayerID", required = false) String payerId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "La orden fue aprobada en PayPal. Usa el token recibido como PayPal Order ID para capturarla.");
        response.put("paypalOrderId", token);
        response.put("payerId", payerId);
        response.put("capturada", false);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cancel")
    public ResponseEntity<Map<String, Object>> paypalCancel(@RequestParam(value = "token", required = false) String token) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", "El pago fue cancelado por el usuario en PayPal.");
        response.put("paypalOrderId", token);
        return ResponseEntity.ok(response);
    }
}
