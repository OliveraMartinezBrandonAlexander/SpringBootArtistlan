package com.example.demo.service.impl;

import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.repository.CategoriaUsuariosRepository;
import com.example.demo.service.CategoriaUsuariosService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class CategoriaUsuariosServiceImpl extends CategoriaUsuariosService {

    @Autowired
    private final CategoriaUsuariosRepository repo;

    @Override
    public CategoriaUsuarios guardar(CategoriaUsuarios cu) {
        return repo.save(cu);
    }

    @Override
    public List<CategoriaUsuarios> listar() {
        return repo.findAll();
    }

    @Override
    public Optional<CategoriaUsuarios> buscarPorId(CategoriaUsuariosID id) {
        return repo.findById(id);
    }

    @Override
    public void eliminar(CategoriaUsuariosID id) {
        if(repo.existsById(id))
        {
            repo.deleteById(id);
        }
    }
}
