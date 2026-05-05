package com.example.demo.service.impl;

import com.example.demo.dto.ServicioDTO;
import com.example.demo.enums.EstadoCuenta;
import com.example.demo.enums.EstadoModeracion;
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
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ServicioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@RequiredArgsConstructor
public class ServicioServiceImpl implements ServicioService {

    private static final String MENSAJE_SERVICIO_RETIRADO_MODIFICAR = "Este servicio fue retirado por moderacion y no puede modificarse.";
    private static final String MENSAJE_SERVICIO_RETIRADO_ELIMINAR = "Este servicio fue retirado por moderacion y no puede eliminarse desde el portafolio.";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-()\\s]{8,20}$");
    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile("^@?[A-Za-z0-9._]{1,30}$");
    private static final int CATEGORIA_SERVICIO_MIN = 19;
    private static final int CATEGORIA_SERVICIO_MAX = 37;
    private static final List<EstadoModeracion> ESTADOS_NO_VISIBLES_PUBLICO = List.of(
            EstadoModeracion.OCULTO,
            EstadoModeracion.ELIMINADO_POR_MODERACION
    );
    private static final List<EstadoCuenta> ESTADOS_DUENO_NO_VISIBLES_PUBLICO = List.of(
            EstadoCuenta.DESACTIVADO,
            EstadoCuenta.BLOQUEADO_PERMANENTE
    );

    private final ServicioRepository repo;
    private final UsuarioRepository usuarioRepository;
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
    @Transactional(readOnly = true)
    public List<Servicio> listarServiciosPublicosVisibles() {
        List<Servicio> servicios = repo.findPublicosVisibles(
                ESTADOS_NO_VISIBLES_PUBLICO,
                ESTADOS_DUENO_NO_VISIBLES_PUBLICO
        );
        inicializarRelacionesPublicas(servicios);
        return servicios;
    }

    @Override
    public Optional<Servicio> buscarPorId(Integer id) {
        return repo.findByIdConCategoria(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Servicio> buscarServicioPublicoVisiblePorId(Integer id) {
        Optional<Servicio> servicio = repo.findPublicoVisiblePorId(
                id,
                ESTADOS_NO_VISIBLES_PUBLICO,
                ESTADOS_DUENO_NO_VISIBLES_PUBLICO
        );
        servicio.ifPresent(this::inicializarRelacionPublica);
        return servicio;
    }

    @Override
    @Transactional
    public Optional<Servicio> actualizarServicio(Integer id, Servicio servicioActualizado) {
        return repo.findById(id).map(existente -> {
            validarNoRetiradoPorModeracionParaEditar(existente);
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
        Servicio servicio = repo.findById(id).orElse(null);
        if (servicio == null) {
            return false;
        }

        validarNoRetiradoPorModeracionParaEliminar(servicio);
        ocultarServicioLogicamente(servicio, "Servicio eliminado por el usuario");
        return true;
    }

    @Override
    public List<Servicio> buscarPorUsuarioId(Integer usuarioId) {
        return repo.findByUsuarioIdUsuarioAndEstadoModeracionNot(
                usuarioId,
                EstadoModeracion.ELIMINADO_POR_MODERACION
        );
    }

    @Override
    @Transactional
    public void ocultarPorUsuarioId(Integer usuarioId) {
        List<Servicio> servicios = repo.findByUsuarioIdUsuario(usuarioId);
        for (Servicio servicio : servicios) {
            ocultarServicioLogicamente(servicio, "Servicio ocultado por desactivacion de cuenta");
        }
    }

    @Override
    @Transactional
    public Servicio actualizarServicioDeUsuario(Integer usuarioId, Integer idServicio, ServicioDTO dto) {
        Servicio existente = repo.findByIdConCategoria(idServicio)
                .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado con ID: " + idServicio));

        validarPertenencia(existente, usuarioId);
        validarNoRetiradoPorModeracionParaEditar(existente);

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
        validarNoRetiradoPorModeracionParaEliminar(servicio);
        ocultarServicioLogicamente(servicio, "Servicio eliminado por el usuario");
    }

    @Override
    @Transactional
    public Servicio crearServicioParaUsuario(Integer usuarioId, ServicioDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
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
            throw new ResponseStatusException(FORBIDDEN, "El servicio no pertenece al usuario indicado");
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

    private void validarNoRetiradoPorModeracionParaEditar(Servicio servicio) {
        if (servicio != null && servicio.getEstadoModeracion() == EstadoModeracion.ELIMINADO_POR_MODERACION) {
            throw new ResponseStatusException(CONFLICT, MENSAJE_SERVICIO_RETIRADO_MODIFICAR);
        }
    }

    private void validarNoRetiradoPorModeracionParaEliminar(Servicio servicio) {
        if (servicio != null && servicio.getEstadoModeracion() == EstadoModeracion.ELIMINADO_POR_MODERACION) {
            throw new ResponseStatusException(CONFLICT, MENSAJE_SERVICIO_RETIRADO_ELIMINAR);
        }
    }

    private void inicializarRelacionesPublicas(List<Servicio> servicios) {
        for (Servicio servicio : servicios) {
            inicializarRelacionPublica(servicio);
        }
    }

    private void inicializarRelacionPublica(Servicio servicio) {
        if (servicio == null) {
            return;
        }
        if (servicio.getUsuario() != null) {
            servicio.getUsuario().getIdUsuario();
            servicio.getUsuario().getUsuario();
            servicio.getUsuario().getFotoPerfil();
        }
        if (servicio.getCategoriasServicios() != null) {
            servicio.getCategoriasServicios().size();
        }
    }

    private void ocultarServicioLogicamente(Servicio servicio, String motivo) {
        servicio.setOculto(Boolean.TRUE);
        servicio.setEstadoModeracion(EstadoModeracion.OCULTO);
        servicio.setMotivoOculto(motivo);
        servicio.setFechaOculto(LocalDateTime.now());
        repo.save(servicio);
    }
}
