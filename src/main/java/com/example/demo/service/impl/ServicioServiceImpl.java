package com.example.demo.service.impl;

import com.example.demo.dto.ServicioDTO;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;
import com.example.demo.model.Servicio;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CategoriaServiciosRepository;
import com.example.demo.repository.FavoritosRepository;
import com.example.demo.repository.ServicioRepository;
import com.example.demo.service.ServicioService;
import com.example.demo.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServicioServiceImpl implements ServicioService {

    private final ServicioRepository repo;
    private final UsuarioService usuarioService;
    private final CategoriaRepository categoriaRepository;
    private final CategoriaServiciosRepository categoriaServiciosRepository;
    private final FavoritosRepository favoritosRepository;

    @Override
    public Servicio guardarServicio(Servicio s) {
        return repo.save(s);
    }

    @Override
    public List<Servicio> todosServicios() {
        return repo.findAllConCategoria();
    }

    @Override
    public Optional<Servicio> buscarPorId(Integer id) {
        return repo.findByIdConCategoria(id);
    }

    @Override
    @Transactional
    public Optional<Servicio> actualizarServicio(Integer id, Servicio servicioActualizado) {
        return repo.findById(id).map(existente -> {
            existente.setTitulo(servicioActualizado.getTitulo());
            existente.setDescripcion(servicioActualizado.getDescripcion());
            existente.setContacto(servicioActualizado.getContacto());
            existente.setTecnicas(servicioActualizado.getTecnicas());
            return repo.save(existente);
        });
    }

    @Override
    @Transactional
    public boolean eliminarServicio(Integer id) {
        if (!repo.existsById(id)) {
            return false;
        }

        validarSinRelacionesParaEliminar(id);
        repo.deleteById(id);
        return true;
    }

    @Override
    public List<Servicio> buscarPorUsuarioId(Integer usuarioId) {
        return repo.findByUsuarioIdUsuario(usuarioId);
    }

    @Override
    @Transactional
    public Servicio actualizarServicioDeUsuario(Integer usuarioId, Integer idServicio, ServicioDTO dto) {
        Servicio existente = repo.findByIdConCategoria(idServicio)
                .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado con ID: " + idServicio));

        validarPertenencia(existente, usuarioId);
        aplicarCamposEditables(existente, dto);
        reemplazarCategoria(existente, dto.getIdCategoria());

        Servicio guardado = repo.save(existente);
        return repo.findByIdConCategoria(guardado.getIdServicio()).orElse(guardado);
    }

    @Override
    @Transactional
    public void eliminarServicioDeUsuario(Integer usuarioId, Integer idServicio) {
        Servicio servicio = repo.findById(idServicio)
                .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado con ID: " + idServicio));

        validarPertenencia(servicio, usuarioId);
        validarSinRelacionesParaEliminar(idServicio);
        repo.delete(servicio);
    }

    @Override
    @Transactional
    public Servicio crearServicioParaUsuario(Integer usuarioId, ServicioDTO dto) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + usuarioId));

        Servicio servicio = new Servicio();
        aplicarCamposEditables(servicio, dto);
        servicio.setUsuario(usuario);

        Servicio guardado = repo.save(servicio);
        reemplazarCategoria(guardado, dto.getIdCategoria());

        return repo.findByIdConCategoria(guardado.getIdServicio()).orElse(guardado);
    }

    private void aplicarCamposEditables(Servicio destino, ServicioDTO origen) {
        destino.setTitulo(origen.getTitulo());
        destino.setDescripcion(origen.getDescripcion());
        destino.setContacto(origen.getContacto());
        destino.setTecnicas(origen.getTecnicas());
    }

    private void reemplazarCategoria(Servicio servicio, Integer idCategoria) {
        if (idCategoria == null) {
            return;
        }

        categoriaServiciosRepository.deleteByServicioIdServicio(servicio.getIdServicio());
        servicio.getCategoriasServicios().clear();

        if (idCategoria <= 0) {
            return;
        }

        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new NoSuchElementException("Categoria no encontrada con ID: " + idCategoria));

        CategoriaServicios categoriaServicio = new CategoriaServicios(
                new CategoriaServiciosID(servicio.getIdServicio(), categoria.getIdCategoria()),
                servicio,
                categoria
        );

        servicio.getCategoriasServicios().add(categoriaServicio);
    }

    private void validarPertenencia(Servicio servicio, Integer usuarioId) {
        if (servicio.getUsuario() == null || !usuarioId.equals(servicio.getUsuario().getIdUsuario())) {
            throw new SecurityException("El servicio no pertenece al usuario indicado");
        }
    }

    private void validarSinRelacionesParaEliminar(Integer idServicio) {
        if (favoritosRepository.existsByServicioIdServicio(idServicio)) {
            throw new IllegalStateException("El servicio no se puede eliminar porque tiene favoritos relacionados");
        }
    }
}
