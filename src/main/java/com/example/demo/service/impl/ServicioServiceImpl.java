package com.example.demo.service.impl;

import com.example.demo.dto.ServicioDTO;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.Servicio;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CategoriaServiciosRepository;
import com.example.demo.repository.ServicioRepository;
import com.example.demo.service.ServicioService;
import com.example.demo.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServicioServiceImpl implements ServicioService {

    @Autowired
    private ServicioRepository repo;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CategoriaServiciosRepository categoriaServiciosRepository;

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
    public Optional<Servicio> actualizarServicio(Integer id, Servicio servicioActualizado) {
        return repo.findById(id).map(s -> {
            s.setTitulo(servicioActualizado.getTitulo());
            s.setDescripcion(servicioActualizado.getDescripcion());
            s.setContacto(servicioActualizado.getContacto());
            s.setTecnicas(servicioActualizado.getTecnicas());
            // usuario / categorías se podrían actualizar en métodos específicos
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

    /**
     * Crea un servicio para un usuario y registra la relación en categoria_servicios.
     */
    @Override
    public Servicio crearServicioParaUsuario(Integer usuarioId, ServicioDTO dto) {

        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Servicio servicio = new Servicio();
        servicio.setTitulo(dto.getTitulo());
        servicio.setDescripcion(dto.getDescripcion());
        servicio.setContacto(dto.getContacto());
        servicio.setTecnicas(dto.getTecnicas());
        servicio.setUsuario(usuario);

        // Primero guardamos el servicio
        Servicio guardado = repo.save(servicio);

        // Luego creamos el registro en categoria_servicios si viene idCategoria
        if (dto.getIdCategoria() != null) {
            Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

            CategoriaServicios cs = new CategoriaServicios();
            cs.setServicio(guardado);
            cs.setCategoria(categoria);

            categoriaServiciosRepository.save(cs);
        }

        return guardado;
    }
}
