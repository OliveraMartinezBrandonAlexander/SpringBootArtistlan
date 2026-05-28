package com.example.demo.service.impl;

import com.example.demo.dto.ConvocatoriaDTO;
import com.example.demo.model.Convocatoria;
import com.example.demo.repository.ConvocatoriaRepository;
import com.example.demo.service.ConvocatoriaService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
                    if (dto.getEstado() != null) {
                        existente.setEstado(dto.getEstado());
                    }
                    if (dto.getPublicada() != null) {
                        existente.setPublicada(dto.getPublicada());
                    }
                    if (dto.getFechaPublicacion() != null) {
                        existente.setFechaPublicacion(dto.getFechaPublicacion());
                    }
                    normalizarPublicacion(existente);
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
                .estado(c.getEstado())
                .publicada(c.getPublicada())
                .fechaPublicacion(c.getFechaPublicacion())
                .fechaCreacion(c.getFechaCreacion())
                .fechaActualizacion(c.getFechaActualizacion())
                .build();
    }

    private Convocatoria convertirAEntidad(ConvocatoriaDTO dto) {
        Convocatoria convocatoria = Convocatoria.builder()
                .idConvocatoria(dto.getIdConvocatoria())
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .fecha(dto.getFecha())
                .enlace(dto.getEnlace())
                .estado(dto.getEstado())
                .publicada(dto.getPublicada())
                .fechaPublicacion(dto.getFechaPublicacion())
                .fechaCreacion(dto.getFechaCreacion())
                .fechaActualizacion(dto.getFechaActualizacion())
                .build();

        normalizarPublicacion(convocatoria);
        return convocatoria;
    }

    private void normalizarPublicacion(Convocatoria convocatoria) {
        boolean estadoPublicado = "PUBLICADA".equalsIgnoreCase(convocatoria.getEstado());

        if (Boolean.TRUE.equals(convocatoria.getPublicada())) {
            convocatoria.setEstado("PUBLICADA");
        } else if (estadoPublicado) {
            convocatoria.setPublicada(true);
        }

        if ((Boolean.TRUE.equals(convocatoria.getPublicada()) || estadoPublicado)
                && convocatoria.getFechaPublicacion() == null) {
            convocatoria.setFechaPublicacion(LocalDateTime.now());
        }
    }
}
