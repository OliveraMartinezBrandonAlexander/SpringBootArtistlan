package com.example.demo.service;

import com.example.demo.model.Servicio;
import java.util.List;
import java.util.Optional;

public interface ServicioService {
    Servicio guardarServicio(Servicio s);
    List<Servicio> todosServicios();
    Optional<Servicio> buscarPorId(Integer id);
    Optional<Servicio> actualizarServicio(Integer id, Servicio servicio);
    boolean eliminarServicio(Integer id);
}