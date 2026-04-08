package com.example.demo.service;

import com.example.demo.dto.CarritoDTO;
import com.example.demo.dto.CarritoContactoDTO;
import com.example.demo.dto.CarritoRequestDTO;
import com.example.demo.dto.CarritoTotalDTO;

import java.util.List;

public interface CarritoService {

    CarritoDTO agregarAlCarrito(CarritoRequestDTO request);

    List<CarritoDTO> obtenerCarritoUsuario(Integer idUsuario);

    List<CarritoDTO> listarObrasEnCarrito(Integer idUsuario);

    void eliminarDelCarrito(CarritoRequestDTO request);

    void limpiarCarritoUsuario(Integer idUsuario);

    CarritoTotalDTO obtenerTotal(Integer idUsuario);

    CarritoContactoDTO obtenerContactoVendedor(Integer idUsuario, Integer idObra);

    int limpiarReservasVencidas();
}
