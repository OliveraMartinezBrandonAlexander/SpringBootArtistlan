package com.example.demo.service.impl;

import com.example.demo.dto.ServicioDTO;
import com.example.demo.exception.BusinessException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ServicioServiceImpl implements ServicioService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-()\\s]{8,20}$");
    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile("^@?[A-Za-z0-9._]{1,30}$");
    private static final int CATEGORIA_SERVICIO_MIN = 19;
    private static final int CATEGORIA_SERVICIO_MAX = 37;

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
            existente.setTipoContacto(servicioActualizado.getTipoContacto());
            existente.setContacto(servicioActualizado.getContacto());
            existente.setTecnicas(servicioActualizado.getTecnicas());
            validarContacto(existente.getTipoContacto(), existente.getContacto());
            return repo.save(existente);
        });
    }

    @Override
    @Transactional
    public boolean eliminarServicio(Integer id) {
        if (!repo.existsById(id)) {
            return false;
        }

        favoritosRepository.deleteByServicioIdServicio(id);
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

        if (dto.getTitulo() != null) {
            existente.setTitulo(dto.getTitulo());
        }
        if (dto.getDescripcion() != null) {
            existente.setDescripcion(dto.getDescripcion());
        }
        if (dto.getTipoContacto() != null) {
            existente.setTipoContacto(dto.getTipoContacto());
        }
        if (dto.getContacto() != null) {
            existente.setContacto(dto.getContacto());
        }
        if (dto.getTecnicas() != null) {
            existente.setTecnicas(dto.getTecnicas());
        }

        validarContacto(existente.getTipoContacto(), existente.getContacto());

        if (dto.getPrecioMin() != null || dto.getPrecioMax() != null) {
            validarRangoPrecios(dto.getPrecioMin(), dto.getPrecioMax());
        }

        if (dto.getIdCategoria() != null && dto.getIdCategoria() > 0) {
            reemplazarCategoria(existente, dto.getIdCategoria());
        }

        Servicio guardado = repo.save(existente);
        return repo.findByIdConCategoria(guardado.getIdServicio()).orElse(guardado);
    }

    @Override
    @Transactional
    public void eliminarServicioDeUsuario(Integer usuarioId, Integer idServicio) {
        Servicio servicio = repo.findById(idServicio)
                .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado con ID: " + idServicio));

        validarPertenencia(servicio, usuarioId);
        favoritosRepository.deleteByServicioIdServicio(idServicio);
        repo.delete(servicio);
    }

    @Override
    @Transactional
    public Servicio crearServicioParaUsuario(Integer usuarioId, ServicioDTO dto) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + usuarioId));

        validarContacto(dto.getTipoContacto(), dto.getContacto());
        validarRangoPrecios(dto.getPrecioMin(), dto.getPrecioMax());

        Servicio servicio = new Servicio();
        servicio.setTitulo(dto.getTitulo());
        servicio.setDescripcion(dto.getDescripcion());
        servicio.setTipoContacto(dto.getTipoContacto());
        servicio.setContacto(dto.getContacto());
        servicio.setTecnicas(dto.getTecnicas());
        servicio.setPrecioMin(dto.getPrecioMin());
        servicio.setPrecioMax(dto.getPrecioMax());
        servicio.setUsuario(usuario);

        Servicio guardado = repo.save(servicio);
        reemplazarCategoria(guardado, dto.getIdCategoria());

        return repo.findByIdConCategoria(guardado.getIdServicio()).orElse(guardado);
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
        validarCategoriaServicio(idCategoria);

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

    private void validarRangoPrecios(BigDecimal min, BigDecimal max) {
        boolean ambosNulos = min == null && max == null;
        boolean ambosPresentes = min != null && max != null;
        if (!ambosNulos && !ambosPresentes) {
            throw new BusinessException("precioMin y precioMax deben venir ambos o ninguno");
        }
        if (ambosPresentes && min.compareTo(max) >= 0) {
            throw new BusinessException("precioMin debe ser menor que precioMax");
        }
    }

    private void validarContacto(String tipoContacto, String contacto) {
        if (tipoContacto == null || contacto == null || contacto.isBlank()) {
            throw new BusinessException("tipoContacto y contacto son obligatorios");
        }

        switch (tipoContacto.toUpperCase()) {
            case "EMAIL" -> {
                if (!EMAIL_PATTERN.matcher(contacto).matches()) {
                    throw new BusinessException("Contacto EMAIL invalido");
                }
            }
            case "WHATSAPP", "TELEFONO" -> {
                if (!PHONE_PATTERN.matcher(contacto).matches()) {
                    throw new BusinessException("Contacto telefonico invalido");
                }
            }
            case "INSTAGRAM" -> {
                if (!INSTAGRAM_PATTERN.matcher(contacto).matches()) {
                    throw new BusinessException("Contacto INSTAGRAM invalido");
                }
            }
            case "OTRO" -> {
            }
            default -> throw new BusinessException("tipoContacto no soportado");
        }
    }

    private void validarCategoriaServicio(Integer idCategoria) {
        if (idCategoria < CATEGORIA_SERVICIO_MIN || idCategoria > CATEGORIA_SERVICIO_MAX) {
            throw new BusinessException("La categoria de servicio debe estar entre 19 y 37.");
        }
    }
}
