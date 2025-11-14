package com.example.demo.service;

import com.example.demo.model.CategoriaObras;
import com.example.demo.model.CategoriaObrasID;
import com.example.demo.repository.CategoriaObrasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaObrasService {
    @Autowired
    private CategoriaObrasRepository repo;

    public CategoriaObras guardar(CategoriaObras co) {
        return repo.save(co);
    }

    public List<CategoriaObras> listar() {
        return repo.findAll();
    }

    public Optional<CategoriaObras> buscarPorId(CategoriaObrasID id) {
        return repo.findById(id);
    }

    public void eliminar(CategoriaObrasID id) {
        repo.deleteById(id);
    }
}