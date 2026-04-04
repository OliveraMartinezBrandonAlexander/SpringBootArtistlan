package com.example.demo.service;

import com.example.demo.dto.CapturarOrdenPaypalResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalResponseDTO;

public interface PaypalPagoService {

    CrearOrdenPaypalResponseDTO crearOrdenParaObra(Integer idObra, Integer compradorId);

    CapturarOrdenPaypalResponseDTO capturarOrden(String paypalOrderId);
}
