package com.example.demo.service.impl;

import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;
import com.example.demo.repository.CategoriaServiciosRepository;
import com.example.demo.service.CategoriaServiciosService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CategoriaServiciosServiceImpl implements CategoriaServiciosService {

    private final CategoriaServiciosRepository repo;

    @Override
    public CategoriaServicios guardar(CategoriaServicios cs) {
        return repo.save(cs);
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
        if (repo.existsById(id)) {
            repo.deleteById(id);
        }
    }

    @Override
    public List<CategoriaServicios> buscarPorServicio(Integer idServicio) {
        return repo.findAll().stream()
                .filter(cs -> cs.getId().getIdServicio().equals(idServicio))
                .toList();
    }

    @Override
    public List<CategoriaServicios> buscarPorCategoria(Integer idCategoria) {
        return repo.findAll().stream()
                .filter(cs -> cs.getId().getIdCategoria().equals(idCategoria))
                .toList();
    }
}