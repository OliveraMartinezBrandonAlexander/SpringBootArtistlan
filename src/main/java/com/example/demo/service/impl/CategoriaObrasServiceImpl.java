package com.example.demo.service.impl;

import com.example.demo.model.CategoriaObras;
import com.example.demo.model.CategoriaObrasID;
import com.example.demo.repository.CategoriaObrasRepository;
import com.example.demo.service.CategoriaObrasService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaObrasServiceImpl implements CategoriaObrasService {

    private final CategoriaObrasRepository repo;

    public CategoriaObrasServiceImpl(CategoriaObrasRepository repo) {
        this.repo = repo;
    }

    @Override
    public CategoriaObras guardar(CategoriaObras co) {
        return repo.save(co);
    }

    @Override
    public List<CategoriaObras> listar() {
        return repo.findAll();
    }

    @Override
    public Optional<CategoriaObras> buscarPorId(CategoriaObrasID id) {
        return repo.findById(id);
    }

    @Override
    public void eliminar(CategoriaObrasID id) {
        repo.deleteById(id);
    }
}
