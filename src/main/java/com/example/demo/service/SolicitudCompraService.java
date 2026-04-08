package com.example.demo.service;

import com.example.demo.dto.solicitud.CrearSolicitudCompraRequestDTO;
import com.example.demo.dto.solicitud.SolicitudCompraDTO;

import java.util.List;

public interface SolicitudCompraService {
    SolicitudCompraDTO crearSolicitud(CrearSolicitudCompraRequestDTO request);
    List<SolicitudCompraDTO> listarRecibidas(Integer vendedorId);
    List<SolicitudCompraDTO> listarEnviadas(Integer compradorId);
    SolicitudCompraDTO obtenerDetalle(Integer idSolicitud, Integer actorId);
    SolicitudCompraDTO aceptar(Integer idSolicitud, Integer idVendedor);
    SolicitudCompraDTO rechazar(Integer idSolicitud, Integer idVendedor, String motivo);
    SolicitudCompraDTO cancelar(Integer idSolicitud, Integer idComprador);
    long contarPendientesUsuario(Integer usuarioId);
    int expirarReservasVencidas();
}
