package com.example.demo.service.impl;

import com.example.demo.dto.notificacion.NotificacionDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Notificacion;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacionRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionDTO> listarPorUsuario(Integer idUsuario) {
        return notificacionRepository.findActivasPorUsuario(idUsuario).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacionDTO obtenerDetalle(Integer idUsuario, Integer idNotificacion) {
        Notificacion notificacion = notificacionRepository.findDetalle(idNotificacion, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        return toDto(notificacion);
    }

    @Override
    @Transactional
    public NotificacionDTO marcarLeida(Integer idUsuario, Integer idNotificacion) {
        Notificacion notificacion = notificacionRepository.findDetalle(idNotificacion, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        notificacion.setLeida(true);
        return toDto(notificacionRepository.save(notificacion));
    }

    @Override
    @Transactional
    public void marcarTodasLeidas(Integer idUsuario) {
        notificacionRepository.marcarTodasLeidas(idUsuario);
    }

    @Override
    @Transactional
    public void eliminarLogicamente(Integer idUsuario, Integer idNotificacion) {
        Notificacion notificacion = notificacionRepository.findDetalle(idNotificacion, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        notificacion.setEliminada(true);
        notificacionRepository.save(notificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas(Integer idUsuario) {
        return notificacionRepository.countByUsuarioDestinoIdUsuarioAndLeidaFalseAndEliminadaFalse(idUsuario);
    }

    @Override
    @Transactional
    public void crearNotificacionSistema(Integer idDestino, String tipoNotificacion, String titulo, String mensaje, String referenciaTipo, Integer referenciaId) {
        Usuario destino = usuarioRepository.findById(idDestino)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario destino no encontrado"));

        notificacionRepository.save(Notificacion.builder()
                .usuarioDestino(destino)
                .tipoOrigen("SISTEMA")
                .tipoNotificacion(tipoNotificacion)
                .titulo(titulo)
                .mensaje(mensaje)
                .referenciaTipo(referenciaTipo)
                .referenciaId(referenciaId)
                .build());
    }

    @Override
    @Transactional
    public void crearNotificacionUsuario(Integer idDestino, Integer idOrigen, String tipoNotificacion, String titulo, String mensaje, String referenciaTipo, Integer referenciaId) {
        Usuario destino = usuarioRepository.findById(idDestino)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario destino no encontrado"));

        Usuario origen = idOrigen != null ? usuarioRepository.findById(idOrigen).orElse(null) : null;

        notificacionRepository.save(Notificacion.builder()
                .usuarioDestino(destino)
                .tipoOrigen("USUARIO")
                .usuarioOrigen(origen)
                .tipoNotificacion(tipoNotificacion)
                .titulo(titulo)
                .mensaje(mensaje)
                .referenciaTipo(referenciaTipo)
                .referenciaId(referenciaId)
                .build());
    }

    private NotificacionDTO toDto(Notificacion n) {
        return NotificacionDTO.builder()
                .idNotificacion(n.getIdNotificacion())
                .tipoOrigen(n.getTipoOrigen())
                .idUsuarioOrigen(n.getUsuarioOrigen() != null ? n.getUsuarioOrigen().getIdUsuario() : null)
                .nombreUsuarioOrigen(n.getUsuarioOrigen() != null ? n.getUsuarioOrigen().getUsuario() : null)
                .fotoPerfilOrigen(n.getUsuarioOrigen() != null ? n.getUsuarioOrigen().getFotoPerfil() : null)
                .tipoNotificacion(n.getTipoNotificacion())
                .titulo(n.getTitulo())
                .mensaje(n.getMensaje())
                .referenciaTipo(n.getReferenciaTipo())
                .referenciaId(n.getReferenciaId())
                .leida(n.getLeida())
                .fechaCreacion(n.getFechaCreacion())
                .build();
    }
}
