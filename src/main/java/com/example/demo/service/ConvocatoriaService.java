package com.example.demo.service;

import com.example.demo.dto.ConvocatoriaDTO;

import java.util.List;
import java.util.Optional;

public interface ConvocatoriaService {

    List<ConvocatoriaDTO> listarTodas();

    Optional<ConvocatoriaDTO> obtenerPorId(Integer id);

    ConvocatoriaDTO crear(ConvocatoriaDTO dto);

    Optional<ConvocatoriaDTO> actualizar(Integer id, ConvocatoriaDTO dto);

    boolean eliminar(Integer id);
}