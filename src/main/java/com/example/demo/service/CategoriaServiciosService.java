package com.example.demo.service;

import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;

import java.util.List;
import java.util.Optional;

public interface CategoriaServiciosService {
    CategoriaServicios guardar(CategoriaServicios cs);
    List<CategoriaServicios> listar();
    Optional<CategoriaServicios> buscarPorId(CategoriaServiciosID id);
    void eliminar(CategoriaServiciosID id);
    List<CategoriaServicios> buscarPorServicio(Integer idServicio);
    List<CategoriaServicios> buscarPorCategoria(Integer idCategoria);
}