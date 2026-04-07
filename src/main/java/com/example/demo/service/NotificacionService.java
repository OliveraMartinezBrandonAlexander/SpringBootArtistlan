package com.example.demo.service;

import com.example.demo.dto.notificacion.NotificacionDTO;

import java.util.List;

public interface NotificacionService {
    List<NotificacionDTO> listarPorUsuario(Integer idUsuario);
    NotificacionDTO obtenerDetalle(Integer idUsuario, Integer idNotificacion);
    NotificacionDTO marcarLeida(Integer idUsuario, Integer idNotificacion);
    void marcarTodasLeidas(Integer idUsuario);
    void eliminarLogicamente(Integer idUsuario, Integer idNotificacion);
    long contarNoLeidas(Integer idUsuario);

    void crearNotificacionSistema(Integer idDestino, String tipoNotificacion, String titulo, String mensaje, String referenciaTipo, Integer referenciaId);
    void crearNotificacionUsuario(Integer idDestino, Integer idOrigen, String tipoNotificacion, String titulo, String mensaje, String referenciaTipo, Integer referenciaId);
}
