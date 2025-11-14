package com.example.demo.service;

import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;
import com.example.demo.repository.CategoriaServiciosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaServiciosService {
    @Autowired
    private CategoriaServiciosRepository repo;

    public CategoriaServicios guardar(CategoriaServicios cs) {
        return repo.save(cs);
    }

    public List<CategoriaServicios> listar() {
        return repo.findAll();
    }

    public Optional<CategoriaServicios> buscarPorId(CategoriaServiciosID id) {
        return repo.findById(id);
    }

    public void eliminar(CategoriaServiciosID id) {
        repo.deleteById(id);
    }
}