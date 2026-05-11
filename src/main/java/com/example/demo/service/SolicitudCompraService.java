package com.example.demo.service;

import com.example.demo.dto.solicitud.CrearSolicitudCompraRequestDTO;
import com.example.demo.dto.solicitud.SolicitudCompraDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SolicitudCompraService {
    SolicitudCompraDTO crearSolicitud(CrearSolicitudCompraRequestDTO request);
    List<SolicitudCompraDTO> listarRecibidas(Integer vendedorId);
    List<SolicitudCompraDTO> listarEnviadas(Integer compradorId);
    Page<SolicitudCompraDTO> listarRecibidasPaginadas(Integer vendedorId, String estado, Pageable pageable);
    Page<SolicitudCompraDTO> listarEnviadasPaginadas(Integer compradorId, String estado, Pageable pageable);
    SolicitudCompraDTO obtenerDetalle(Integer idSolicitud, Integer actorId);
    SolicitudCompraDTO aceptar(Integer idSolicitud, Integer idVendedor);
    SolicitudCompraDTO rechazar(Integer idSolicitud, Integer idVendedor, String motivo);
    SolicitudCompraDTO cancelar(Integer idSolicitud, Integer idComprador);
    long contarPendientesUsuario(Integer usuarioId);
    int expirarReservasVencidas();
}
