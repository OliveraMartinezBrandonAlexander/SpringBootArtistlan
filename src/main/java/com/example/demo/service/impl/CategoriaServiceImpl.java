package com.example.demo.service.impl;

import com.example.demo.model.Categoria;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    @Autowired
    private CategoriaRepository repo;

    @Override
    public Categoria guardar(Categoria c) {
        return repo.save(c);
    }

    @Override
    public List<Categoria> listar() {
        return repo.findAll();
    }

    @Override
    public Optional<Categoria> buscarPorId(Integer id) {
        return repo.findById(id);
    }
    @Override
    public Optional<Categoria> actualizar(Integer id, Categoria datos) {
        return repo.findById(id).map(categoriaExistente -> {
            categoriaExistente.setNombreCategoria(datos.getNombreCategoria());
            return repo.save(categoriaExistente);
        });
    }
    @Override
    public boolean eliminar(Integer id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
            return true;
        }
        return false;
    }
}