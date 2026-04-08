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
        validarUsuarioExiste(idUsuario);
        return notificacionRepository.findActivasPorUsuario(idUsuario).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacionDTO obtenerDetalle(Integer idUsuario, Integer idNotificacion) {
        validarUsuarioExiste(idUsuario);
        Notificacion notificacion = notificacionRepository.findDetalle(idNotificacion, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada"));
        return toDto(notificacion);
    }

    @Override
    @Transactional
    public NotificacionDTO marcarLeida(Integer idUsuario, Integer idNotificacion) {
        validarUsuarioExiste(idUsuario);
        Notificacion notificacion = notificacionRepository.findDetalle(idNotificacion, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada"));
        notificacion.setLeida(true);
        return toDto(notificacionRepository.save(notificacion));
    }

    @Override
    @Transactional
    public void marcarTodasLeidas(Integer idUsuario) {
        validarUsuarioExiste(idUsuario);
        notificacionRepository.marcarTodasLeidas(idUsuario);
    }

    @Override
    @Transactional
    public void eliminarLogicamente(Integer idUsuario, Integer idNotificacion) {
        validarUsuarioExiste(idUsuario);
        Notificacion notificacion = notificacionRepository.findDetalle(idNotificacion, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada"));
        notificacion.setEliminada(true);
        notificacionRepository.save(notificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas(Integer idUsuario) {
        validarUsuarioExiste(idUsuario);
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

    private NotificacionDTO toDto(Notificacion notificacion) {
        Usuario usuarioOrigen = notificacion.getUsuarioOrigen();
        String usuarioOrigenLogin = null;
        String nombreOrigen = null;
        String fotoOrigen = null;

        if (usuarioOrigen != null) {
            usuarioOrigenLogin = usuarioOrigen.getUsuario();
            nombreOrigen = usuarioOrigen.getNombreCompleto();
            if (nombreOrigen == null || nombreOrigen.isBlank()) {
                nombreOrigen = usuarioOrigenLogin;
            }
            fotoOrigen = usuarioOrigen.getFotoPerfil();
        }

        return NotificacionDTO.builder()
                .idNotificacion(notificacion.getIdNotificacion())
                .tipoOrigen(notificacion.getTipoOrigen())
                .idUsuarioOrigen(usuarioOrigen != null ? usuarioOrigen.getIdUsuario() : null)
                .usuarioOrigen(usuarioOrigenLogin)
                .nombreOrigen(nombreOrigen)
                .fotoOrigen(fotoOrigen)
                .nombreUsuarioOrigen(nombreOrigen)
                .fotoPerfilOrigen(fotoOrigen)
                .tipoNotificacion(notificacion.getTipoNotificacion())
                .titulo(notificacion.getTitulo())
                .mensaje(notificacion.getMensaje())
                .referenciaTipo(notificacion.getReferenciaTipo())
                .referenciaId(notificacion.getReferenciaId())
                .leida(notificacion.getLeida())
                .fechaCreacion(notificacion.getFechaCreacion())
                .build();
    }

    private void validarUsuarioExiste(Integer idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
    }
}
