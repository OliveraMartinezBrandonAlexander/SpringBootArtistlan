package com.example.demo.service;

import com.example.demo.dto.TransaccionDetalleDTO;
import com.example.demo.dto.TransaccionResumenDTO;

import java.util.List;

public interface TransaccionService {

    List<TransaccionResumenDTO> obtenerComprasUsuario(Integer idUsuario);

    List<TransaccionResumenDTO> obtenerVentasUsuario(Integer idUsuario);

    TransaccionDetalleDTO obtenerDetalleTransaccion(Integer idUsuario, String tipoOrigen, Integer idTransaccion);
}
