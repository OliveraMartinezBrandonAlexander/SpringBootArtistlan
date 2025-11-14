package com.example.demo.service.impl;
import com.example.demo.model.Servicio;
import com.example.demo.repository.ServicioRepository;
import com.example.demo.service.ServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServicioServiceImpl implements ServicioService {

    @Autowired
    private ServicioRepository repo;

    @Override
    public Servicio guardarServicio(Servicio s) {
        return repo.save(s);
    }

    @Override
    public List<Servicio> todosServicios() {
        return repo.findAll();
    }

    @Override
    public Optional<Servicio> buscarPorId(Integer id) {
        return repo.findById(id);
    }

    @Override
    public Optional<Servicio> actualizarServicio(Integer id, Servicio servicio) {
        return repo.findById(id).map(s -> {
            s.setTitulo(servicio.getTitulo());
            s.setDescripcion(servicio.getDescripcion());
            s.setContacto(servicio.getContacto());
            return repo.save(s);
        });
    }

    @Override
    public boolean eliminarServicio(Integer id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
            return true;
        }
        return false;
    }
}
