package com.example.demo.service.impl;

import com.example.demo.dto.ConvocatoriaDTO;
import com.example.demo.model.Convocatoria;
import com.example.demo.repository.ConvocatoriaRepository;
import com.example.demo.service.ConvocatoriaService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ConvocatoriaServiceImpl implements ConvocatoriaService {

    private final ConvocatoriaRepository convocatoriaRepository;

    @Override
    public List<ConvocatoriaDTO> listarTodas() {
        return convocatoriaRepository.findAll().stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Override
    public Optional<ConvocatoriaDTO> obtenerPorId(Integer id) {
        return convocatoriaRepository.findById(id).map(this::convertirADTO);
    }

    @Override
    public ConvocatoriaDTO crear(ConvocatoriaDTO dto) {
        Convocatoria guardada = convocatoriaRepository.save(convertirAEntidad(dto));
        return convertirADTO(guardada);
    }

    @Override
    public Optional<ConvocatoriaDTO> actualizar(Integer id, ConvocatoriaDTO dto) {
        return convocatoriaRepository.findById(id)
                .map(existente -> {
                    existente.setTitulo(dto.getTitulo());
                    existente.setDescripcion(dto.getDescripcion());
                    existente.setFecha(dto.getFecha());
                    existente.setEnlace(dto.getEnlace());
                    return convocatoriaRepository.save(existente);
                })
                .map(this::convertirADTO);
    }

    @Override
    public boolean eliminar(Integer id) {
        if (!convocatoriaRepository.existsById(id)) {
            return false;
        }
        convocatoriaRepository.deleteById(id);
        return true;
    }

    private ConvocatoriaDTO convertirADTO(Convocatoria c) {
        return ConvocatoriaDTO.builder()
                .idConvocatoria(c.getIdConvocatoria())
                .titulo(c.getTitulo())
                .descripcion(c.getDescripcion())
                .fecha(c.getFecha())
                .enlace(c.getEnlace())
                .build();
    }

    private Convocatoria convertirAEntidad(ConvocatoriaDTO dto) {
        return Convocatoria.builder()
                .idConvocatoria(dto.getIdConvocatoria())
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .fecha(dto.getFecha())
                .enlace(dto.getEnlace())
                .build();
    }
}