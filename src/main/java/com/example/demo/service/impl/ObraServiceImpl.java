package com.example.demo.service.impl;

import com.example.demo.model.Obra;
import com.example.demo.repository.ObraRepository;
import com.example.demo.service.ObraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ObraServiceImpl implements ObraService {

    @Autowired
    private ObraRepository repo;

    @Override
    public Obra guardar(Obra o) {
        return repo.save(o);
    }

    @Override
    public List<Obra> listar() {
        return repo.findAll();
    }

    @Override
    public Optional<Obra> buscarPorId(Integer id) {
        return repo.findById(id);
    }

    @Override
    public Optional<Obra> actualizarObra(Integer id, Obra obra) {
        return repo.findById(id).map(o -> {
            o.setTitulo(obra.getTitulo());
            o.setDescripcion(obra.getDescripcion());
            o.setEstado(obra.getEstado());
            o.setPrecio(obra.getPrecio());
            o.setImagen1(obra.getImagen1());
            o.setImagen2(obra.getImagen2());
            o.setImagen3(obra.getImagen3());
            o.setTecnicas(obra.getTecnicas());
            o.setMedidas(obra.getMedidas());
            o.setLikes(obra.getLikes());
            o.setUsuario(obra.getUsuario());
            return repo.save(o);
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

    @Override
    public Optional<Obra> actualizarImagen1(Integer id, String urlImagen) {
        return repo.findById(id).map(o -> {
            o.setImagen1(urlImagen);
            return repo.save(o);
        });
    }
}