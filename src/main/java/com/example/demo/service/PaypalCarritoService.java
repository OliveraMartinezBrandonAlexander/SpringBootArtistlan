package com.example.demo.service;

import com.example.demo.dto.CapturarOrdenPaypalCarritoResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalCarritoResponseDTO;

public interface PaypalCarritoService {

    CrearOrdenPaypalCarritoResponseDTO crearOrdenParaCarrito(Integer idUsuario);

    CapturarOrdenPaypalCarritoResponseDTO capturarOrdenCarrito(String paypalOrderId);
}
