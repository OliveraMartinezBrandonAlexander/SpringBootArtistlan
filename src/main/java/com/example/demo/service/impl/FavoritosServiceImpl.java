package com.example.demo.service.impl;

import com.example.demo.model.Favoritos;
import com.example.demo.repository.FavoritosRepository;
import com.example.demo.service.FavoritosService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FavoritosServiceImpl implements FavoritosService {

    private final FavoritosRepository repo;

    public FavoritosServiceImpl(FavoritosRepository repo) {
        this.repo = repo;
    }

    @Override
    public Favoritos guardar(Favoritos f) {
        return repo.save(f);
    }

    @Override
    public List<Favoritos> listar() {
        return repo.findAll();
    }

    @Override
    public Optional<Favoritos> buscarPorId(Integer id) {
        return repo.findById(id);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }
}
