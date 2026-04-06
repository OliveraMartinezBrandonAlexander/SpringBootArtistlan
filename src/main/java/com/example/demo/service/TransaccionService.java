package com.example.demo.service;

import com.example.demo.dto.TransaccionResumenDTO;

import java.util.List;

public interface TransaccionService {

    List<TransaccionResumenDTO> obtenerComprasUsuario(Integer idUsuario);

    List<TransaccionResumenDTO> obtenerVentasUsuario(Integer idUsuario);
}
