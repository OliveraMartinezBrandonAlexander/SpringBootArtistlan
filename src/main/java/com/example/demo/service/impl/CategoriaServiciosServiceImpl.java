package com.example.demo.service.impl;

import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;
import com.example.demo.repository.CategoriaServiciosRepository;
import com.example.demo.service.CategoriaServiciosService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class CategoriaServiciosServiceImpl extends CategoriaServiciosService {

    @Autowired
    private final CategoriaServiciosRepository repo;

    @Override
    public CategoriaServicios guardar(CategoriaServicios cu) {
        return repo.save(cu);
    }

    @Override
    public List<CategoriaServicios> listar() {
        return repo.findAll();
    }

    @Override
    public Optional<CategoriaServicios> buscarPorId(CategoriaServiciosID id) {
        return repo.findById(id);
    }

    @Override
    public void eliminar(CategoriaServiciosID id) {
        if(repo.existsById(id))
        {
            repo.deleteById(id);
        }
    }
}
