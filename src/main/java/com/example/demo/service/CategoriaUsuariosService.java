package com.example.demo.service;

import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.repository.CategoriaUsuariosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaUsuariosService {
    @Autowired
    private CategoriaUsuariosRepository repo;

    public CategoriaUsuarios guardar(CategoriaUsuarios cu) {
        return repo.save(cu);
    }

    public List<CategoriaUsuarios> listar() {
        return repo.findAll();
    }

    public Optional<CategoriaUsuarios> buscarPorId(CategoriaUsuariosID id) {
        return repo.findById(id);
    }

    public void eliminar(CategoriaUsuariosID id) {
        repo.deleteById(id);
    }
}