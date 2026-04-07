package com.example.demo.service;

import com.example.demo.dto.CarritoDTO;
import com.example.demo.dto.CarritoRequestDTO;
import com.example.demo.dto.CarritoTotalDTO;

import java.util.List;

public interface CarritoService {

    CarritoDTO agregarAlCarrito(CarritoRequestDTO request);

    List<CarritoDTO> obtenerCarritoUsuario(Integer idUsuario);

    void eliminarDelCarrito(CarritoRequestDTO request);

    CarritoTotalDTO obtenerTotal(Integer idUsuario);

    int limpiarReservasVencidas();
}
