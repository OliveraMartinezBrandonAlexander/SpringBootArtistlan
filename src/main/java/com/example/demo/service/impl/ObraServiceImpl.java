package com.example.demo.service.impl;

import com.example.demo.dto.ObraDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.ObraService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ObraServiceImpl implements ObraService {

    private static final String ESTADO_EN_VENTA = "EN_VENTA";
    private static final String ESTADO_RESERVADA = "RESERVADA";
    private static final String ESTADO_VENDIDA = "VENDIDA";

    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaObrasRepository categoriaObraRepository;
    private final CategoriaRepository categoriaRepository;
    private final FavoritosRepository favoritosRepository;
    private final CarritoRepository carritoRepository;
    private final CompraObraRepository compraObraRepository;
    private final CompraCarritoDetalleRepository compraCarritoDetalleRepository;
    private final SolicitudCompraObraRepository solicitudRepository;

    @Override
    public Obra guardar(Obra o) {
        return obraRepository.save(o);
    }

    @Override
    public List<Obra> listar() {
        return obraRepository.findAll();
    }

    @Override
    public Optional<Obra> buscarPorId(Integer id) {
        return obraRepository.findByIdConCategoria(id);
    }

    @Override
    @Transactional
    public Optional<Obra> actualizarObra(Integer id, Obra obra) {
        return obraRepository.findById(id).map(existente -> {
            validarEstadoParaEdicion(existente);
            existente.setTitulo(obra.getTitulo());
            existente.setDescripcion(obra.getDescripcion());
            existente.setEstado(obra.getEstado());
            // precio intencionalmente no editable
            existente.setImagen1(obra.getImagen1());
            existente.setImagen2(obra.getImagen2());
            existente.setImagen3(obra.getImagen3());
            existente.setTecnicas(obra.getTecnicas());
            existente.setMedidas(obra.getMedidas());
            return obraRepository.save(existente);
        });
    }

    @Override
    @Transactional
    public Obra guardarObraConCategoria(Integer usuarioId, ObraDTO obraDTO) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + usuarioId));

        Obra obra = new Obra();
        aplicarCamposCreacion(obra, obraDTO);
        obra.setUsuario(usuario);
        obra.setFechaPublicacion(LocalDateTime.now());

        Obra obraGuardada = obraRepository.save(obra);
        reemplazarCategoria(obraGuardada, obraDTO.getIdCategoria());

        return obraRepository.findByIdConCategoria(obraGuardada.getIdObra()).orElse(obraGuardada);
    }

    @Override
    @Transactional
    public Obra actualizarObraDeUsuario(Integer usuarioId, Integer obraId, ObraDTO obraDTO) {
        Obra obraExistente = obraRepository.findByIdConCategoria(obraId)
                .orElseThrow(() -> new NoSuchElementException("Obra no encontrada con ID: " + obraId));

        validarPertenencia(obraExistente, usuarioId);
        validarEstadoParaEdicion(obraExistente);

        obraExistente.setTitulo(obraDTO.getTitulo());
        obraExistente.setDescripcion(obraDTO.getDescripcion());
        obraExistente.setImagen1(obraDTO.getImagen1());
        obraExistente.setImagen2(obraDTO.getImagen2());
        obraExistente.setImagen3(obraDTO.getImagen3());
        obraExistente.setTecnicas(obraDTO.getTecnicas());
        obraExistente.setMedidas(obraDTO.getMedidas());
        obraExistente.setConfirmacionAutoria(obraDTO.getConfirmacionAutoria() != null ? obraDTO.getConfirmacionAutoria() : obraExistente.getConfirmacionAutoria());

        if (!"VENDIDA".equalsIgnoreCase(obraExistente.getEstado()) && !"RESERVADA".equalsIgnoreCase(obraExistente.getEstado())) {
            obraExistente.setEstado(obraDTO.getEstado());
        }

        reemplazarCategoria(obraExistente, obraDTO.getIdCategoria());

        Obra guardada = obraRepository.save(obraExistente);
        return obraRepository.findByIdConCategoria(guardada.getIdObra()).orElse(guardada);
    }

    @Override
    @Transactional
    public void eliminarObraDeUsuario(Integer usuarioId, Integer obraId) {
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new NoSuchElementException("Obra no encontrada con ID: " + obraId));

        validarPertenencia(obra, usuarioId);
        validarEstadoParaEliminar(obra);
        cancelarSolicitudesPendientes(obra.getIdObra());
        obraRepository.delete(obra);
    }

    @Override
    @Transactional
    public boolean eliminar(Integer id) {
        Obra obra = obraRepository.findById(id).orElse(null);
        if (obra == null) {
            return false;
        }
        validarEstadoParaEliminar(obra);
        cancelarSolicitudesPendientes(id);
        obraRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<Obra> actualizarImagen1(Integer id, String urlImagen) {
        return obraRepository.findById(id).map(o -> {
            o.setImagen1(urlImagen);
            return obraRepository.save(o);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Obra> buscarPorUsuarioId(Integer usuarioId) {
        List<Obra> obras = obraRepository.findByUsuarioIdUsuario(usuarioId);
        for (Obra obra : obras) {
            if (obra.getCategoriaObras() != null) {
                obra.getCategoriaObras().size();
            }
        }
        return obras;
    }

    @Override
    @Transactional
    public void eliminarPorUsuarioId(Integer usuarioId) {
        obraRepository.deleteByUsuarioIdUsuario(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaDisponibleParaVenta(Integer idObra) {
        Obra obra = obraRepository.findById(idObra)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada con ID: " + idObra));
        return ESTADO_EN_VENTA.equalsIgnoreCase(obra.getEstado());
    }

    @Override
    @Transactional
    public Obra marcarComoVendida(Integer idObra) {
        Obra obra = obraRepository.findById(idObra)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada con ID: " + idObra));
        obra.setEstado(ESTADO_VENDIDA);
        return obraRepository.save(obra);
    }

    private void aplicarCamposCreacion(Obra destino, ObraDTO origen) {
        destino.setTitulo(origen.getTitulo());
        destino.setDescripcion(origen.getDescripcion());
        destino.setEstado(origen.getEstado() != null ? origen.getEstado() : ESTADO_EN_VENTA);
        destino.setPrecio(origen.getPrecio());
        destino.setImagen1(origen.getImagen1());
        destino.setImagen2(origen.getImagen2());
        destino.setImagen3(origen.getImagen3());
        destino.setTecnicas(origen.getTecnicas());
        destino.setMedidas(origen.getMedidas());
        destino.setConfirmacionAutoria(Boolean.TRUE.equals(origen.getConfirmacionAutoria()));
    }

    private void reemplazarCategoria(Obra obra, Integer idCategoria) {
        if (idCategoria == null) {
            return;
        }

        categoriaObraRepository.deleteByObraIdObra(obra.getIdObra());
        obra.getCategoriaObras().clear();

        if (idCategoria <= 0) {
            return;
        }

        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new NoSuchElementException("Categoria no encontrada con ID: " + idCategoria));

        CategoriaObras categoriaObra = new CategoriaObras(
                new CategoriaObrasID(obra.getIdObra(), categoria.getIdCategoria()),
                obra,
                categoria
        );

        obra.getCategoriaObras().add(categoriaObra);
    }

    private void validarPertenencia(Obra obra, Integer usuarioId) {
        if (obra.getUsuario() == null || !usuarioId.equals(obra.getUsuario().getIdUsuario())) {
            throw new SecurityException("La obra no pertenece al usuario indicado");
        }
    }

    private void validarEstadoParaEdicion(Obra obra) {
        if (ESTADO_RESERVADA.equalsIgnoreCase(obra.getEstado()) || ESTADO_VENDIDA.equalsIgnoreCase(obra.getEstado())) {
            throw new BusinessException("La obra no puede editarse en estado " + obra.getEstado());
        }
    }

    private void validarEstadoParaEliminar(Obra obra) {
        if (ESTADO_RESERVADA.equalsIgnoreCase(obra.getEstado()) || ESTADO_VENDIDA.equalsIgnoreCase(obra.getEstado())) {
            throw new BusinessException("La obra no puede eliminarse en estado " + obra.getEstado());
        }
    }

    private void cancelarSolicitudesPendientes(Integer obraId) {
        List<SolicitudCompraObra> activas = solicitudRepository.findByObraIdObraAndEstadoSolicitudIn(
                obraId,
                List.of("PENDIENTE", "ACEPTADA", "RECHAZADA")
        );
        for (SolicitudCompraObra solicitud : activas) {
            if (!"PAGADA".equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
                solicitud.setEstadoSolicitud("CANCELADA");
                solicitud.setFechaRespuesta(LocalDateTime.now());
                solicitud.setMotivoRechazo("Obra eliminada");
            }
        }
        solicitudRepository.saveAll(activas);
    }
}
