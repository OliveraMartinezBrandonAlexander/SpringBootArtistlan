package com.example.demo.service;

import com.example.demo.dto.ServicioDTO;
import com.example.demo.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ServicioService {

    Servicio guardarServicio(Servicio s);

    List<Servicio> todosServicios();

    List<Servicio> listarServiciosPublicosVisibles();

    Page<Servicio> listarServiciosPublicosVisiblesPaginado(String q, String categoria, Integer idCategoria, Pageable pageable);

    Optional<Servicio> buscarPorId(Integer id);

    Optional<Servicio> buscarServicioPublicoVisiblePorId(Integer id);

    Optional<Servicio> buscarDetalleVisibleOPropioPorId(Integer id, Integer usuarioId);

    Optional<Servicio> actualizarServicio(Integer id, ServicioDTO servicio);

    boolean eliminarServicio(Integer id);

    Servicio actualizarServicioDeUsuario(Integer usuarioId, Integer idServicio, ServicioDTO dto);

    void eliminarServicioDeUsuario(Integer usuarioId, Integer idServicio);

    List<Servicio> buscarPorUsuarioId(Integer usuarioId);

    void ocultarPorUsuarioId(Integer usuarioId);

    Servicio crearServicioParaUsuario(Integer usuarioId, ServicioDTO dto);
}
