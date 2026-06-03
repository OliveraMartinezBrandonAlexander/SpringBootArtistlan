package com.example.demo.service.impl;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dto.ServicioDTO;
import com.example.demo.enums.EstadoCuenta;
import com.example.demo.enums.EstadoModeracion;
import com.example.demo.enums.TipoMetaPersonal;
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
import com.example.demo.service.MetaPersonalService;
import com.example.demo.service.ServicioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private final MetaPersonalService metaPersonalService;

    @Override
    public Servicio guardarServicio(Servicio s) {
        Integer idUsuarioAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        Integer idUsuarioSolicitado = s != null && s.getUsuario() != null ? s.getUsuario().getIdUsuario() : null;
        if (idUsuarioSolicitado != null && !idUsuarioSolicitado.equals(idUsuarioAutenticado)) {
            throw new ResponseStatusException(FORBIDDEN, "No puedes publicar un servicio para otro usuario.");
        }
        Usuario usuarioAutenticado = usuarioRepository.findById(idUsuarioAutenticado)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + idUsuarioAutenticado));
        s.setUsuario(usuarioAutenticado);
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
    @Transactional(readOnly = true)
    public Page<Servicio> listarServiciosPublicosVisiblesPaginado(String q, String categoria, Integer idCategoria, Pageable pageable) {
        Page<Servicio> page = repo.findPublicosVisiblesPaginado(
                ESTADOS_NO_VISIBLES_PUBLICO,
                ESTADOS_DUENO_NO_VISIBLES_PUBLICO,
                q,
                categoria,
                idCategoria,
                pageable
        );
        inicializarRelacionesPublicas(page.getContent());
        return page;
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
    @Transactional(readOnly = true)
    public Optional<Servicio> buscarDetalleVisibleOPropioPorId(Integer id, Integer usuarioId) {
        log.info("ServicioCrudBackendDebug GET detalle recibido idServicio={} usuarioId={}", id, usuarioId);

        Optional<Servicio> servicioPublico = buscarServicioPublicoVisiblePorId(id);
        if (servicioPublico.isPresent()) {
            Servicio servicio = servicioPublico.get();
            Integer propietario = obtenerPropietarioId(servicio);
            log.info("ServicioCrudBackendDebug GET detalle publico encontrado idServicio={} propietario={} usuarioId={}",
                    id, propietario, usuarioId);
            return servicioPublico;
        }

        Optional<Servicio> servicioOpt = repo.findByIdConCategoria(id);
        log.info("ServicioCrudBackendDebug GET detalle existe={} idServicio={}", servicioOpt.isPresent(), id);
        if (servicioOpt.isEmpty()) {
            return Optional.empty();
        }

        Servicio servicio = servicioOpt.get();
        Integer propietario = obtenerPropietarioId(servicio);
        log.info("ServicioCrudBackendDebug GET detalle propietario real idServicio={} propietario={} usuarioId={} oculto={} estadoModeracion={}",
                id, propietario, usuarioId, servicio.getOculto(), servicio.getEstadoModeracion());

        if (estaServicioOcultoOEliminado(servicio)) {
            log.info("ServicioCrudBackendDebug GET detalle 404 por servicio oculto/eliminado idServicio={}", id);
            return Optional.empty();
        }

        if (usuarioId == null) {
            log.info("ServicioCrudBackendDebug GET detalle 404 sin usuarioId para servicio no publico idServicio={}", id);
            return Optional.empty();
        }

        validarPertenencia(servicio, usuarioId);
        inicializarRelacionPublica(servicio);
        log.info("ServicioCrudBackendDebug GET detalle propio editable OK idServicio={} propietario={}", id, propietario);
        return Optional.of(servicio);
    }

    @Override
    @Transactional
    public Optional<Servicio> actualizarServicio(Integer id, ServicioDTO servicioActualizado) {
        Integer idUsuarioAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        return repo.findByIdConCategoria(id).map(existente -> {
            validarNoOcultoOEliminado(existente, id);
            validarPertenencia(existente, idUsuarioAutenticado);
            validarNoRetiradoPorModeracionParaEditar(existente);
            if (servicioActualizado.getIdUsuario() != null && !idUsuarioAutenticado.equals(servicioActualizado.getIdUsuario())) {
                throw new ResponseStatusException(FORBIDDEN, "No puedes editar un servicio de otro usuario.");
            }
            aplicarActualizacionServicio(existente, servicioActualizado);
            return repo.save(existente);
        });
    }

    @Override
    @Transactional
    public boolean eliminarServicio(Integer id) {
        Integer idUsuarioAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        Servicio servicio = repo.findById(id).orElse(null);
        if (servicio == null) {
            return false;
        }
        if (estaServicioOcultoOEliminado(servicio)) {
            log.info("ServicioCrudBackendDebug DELETE /api/servicios/{} no ejecutado: ya oculto/eliminado", id);
            return false;
        }

        validarPertenencia(servicio, idUsuarioAutenticado);
        validarNoRetiradoPorModeracionParaEliminar(servicio);
        ocultarServicioLogicamente(servicio, "Servicio eliminado por el usuario");
        return true;
    }

    @Override
    public List<Servicio> buscarPorUsuarioId(Integer usuarioId) {
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(usuarioId);
        List<Servicio> servicios = repo.findPropiosActivosByUsuarioId(
                idUsuarioAutenticado,
                ESTADOS_NO_VISIBLES_PUBLICO
        );
        log.info("PortafolioBackendDebug listado servicios propios usuarioId={} totalActivos={}", idUsuarioAutenticado, servicios.size());
        return servicios;
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
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(usuarioId);
        log.info("ServicioCrudBackendDebug UPDATE recibido usuarioId={} idServicio={}", idUsuarioAutenticado, idServicio);
        Servicio existente = repo.findByIdConCategoria(idServicio)
                .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado con ID: " + idServicio));

        log.info("ServicioCrudBackendDebug UPDATE existe=true idServicio={} propietario={} usuarioId={} oculto={} estadoModeracion={}",
                idServicio, obtenerPropietarioId(existente), idUsuarioAutenticado, existente.getOculto(), existente.getEstadoModeracion());
        validarNoOcultoOEliminado(existente, idServicio);
        validarPertenencia(existente, idUsuarioAutenticado);
        log.info("ServicioCrudBackendDebug UPDATE validacion propietario OK idServicio={} usuarioId={}", idServicio, idUsuarioAutenticado);
        validarNoRetiradoPorModeracionParaEditar(existente);
        aplicarActualizacionServicio(existente, dto);

        Servicio guardado = repo.save(existente);
        log.info("ServicioCrudBackendDebug UPDATE guardado idServicio={} usuarioId={}", guardado.getIdServicio(), idUsuarioAutenticado);
        return repo.findByIdConCategoria(guardado.getIdServicio()).orElse(guardado);
    }

    @Override
    @Transactional
    public void eliminarServicioDeUsuario(Integer usuarioId, Integer idServicio) {
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(usuarioId);
        log.info("ServicioCrudBackendDebug DELETE recibido usuarioId={} idServicio={}", idUsuarioAutenticado, idServicio);
        Servicio servicio = repo.findById(idServicio)
                .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado con ID: " + idServicio));

        log.info("ServicioCrudBackendDebug DELETE existe=true idServicio={} propietario={} usuarioId={} oculto={} estadoModeracion={}",
                idServicio, obtenerPropietarioId(servicio), idUsuarioAutenticado, servicio.getOculto(), servicio.getEstadoModeracion());
        validarNoOcultoOEliminado(servicio, idServicio);
        validarPertenencia(servicio, idUsuarioAutenticado);
        log.info("ServicioCrudBackendDebug DELETE validacion propietario OK idServicio={} usuarioId={}", idServicio, idUsuarioAutenticado);
        validarNoRetiradoPorModeracionParaEliminar(servicio);
        ocultarServicioLogicamente(servicio, "Servicio eliminado por el usuario");
        int activosDespues = repo.findPropiosActivosByUsuarioId(idUsuarioAutenticado, ESTADOS_NO_VISIBLES_PUBLICO).size();
        log.info("ServicioCrudBackendDebug DELETE soft delete OK idServicio={} usuarioId={} serviciosActivosDespues={}",
                idServicio, idUsuarioAutenticado, activosDespues);
    }

    @Override
    @Transactional
    public Servicio crearServicioParaUsuario(Integer usuarioId, ServicioDTO dto) {
        Integer idUsuarioAutenticado = usuarioId != null
                ? SecurityUtils.validarAccesoUsuario(usuarioId)
                : SecurityUtils.obtenerIdUsuarioAutenticado();
        if (dto.getIdUsuario() != null && !idUsuarioAutenticado.equals(dto.getIdUsuario())) {
            throw new ResponseStatusException(FORBIDDEN, "No puedes publicar un servicio para otro usuario.");
        }
        Usuario usuario = usuarioRepository.findById(idUsuarioAutenticado)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + idUsuarioAutenticado));

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
        servicio.setFechaPublicacion(com.example.demo.util.ArtistlanDateTimeUtils.nowMexico());

        Servicio guardado = repo.save(servicio);
        reemplazarCategoria(guardado, dto.getIdCategoria());
        metaPersonalService.evaluarMetasDelUsuarioPorTipos(idUsuarioAutenticado, EnumSet.of(TipoMetaPersonal.PUBLICACIONES));

        return repo.findByIdConCategoria(guardado.getIdServicio()).orElse(guardado);
    }

    private void reemplazarCategoria(Servicio servicio, Integer idCategoria) {
        if (idCategoria == null) {
            return;
        }

        Integer categoriaActual = servicio.getCategoriasServicios().stream()
                .map(CategoriaServicios::getCategoria)
                .filter(java.util.Objects::nonNull)
                .map(Categoria::getIdCategoria)
                .findFirst()
                .orElse(null);
        if (java.util.Objects.equals(categoriaActual, idCategoria)) {
            return;
        }

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

    private void aplicarActualizacionServicio(Servicio existente, ServicioDTO dto) {
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
            existente.setPrecioMin(dto.getPrecioMin());
            existente.setPrecioMax(dto.getPrecioMax());
        }

        if (dto.getIdCategoria() != null && dto.getIdCategoria() > 0) {
            reemplazarCategoria(existente, dto.getIdCategoria());
        }
    }

    private void validarPertenencia(Servicio servicio, Integer usuarioId) {
        Integer propietario = obtenerPropietarioId(servicio);
        boolean esPropietario = usuarioId != null && usuarioId.equals(propietario);
        log.info("ServicioCrudBackendDebug validacion propietario idServicio={} usuarioId={} propietario={} resultado={}",
                servicio != null ? servicio.getIdServicio() : null, usuarioId, propietario, esPropietario);
        if (!esPropietario) {
            throw new ResponseStatusException(FORBIDDEN, "El servicio no pertenece al usuario indicado");
        }
    }

    private void validarNoOcultoOEliminado(Servicio servicio, Integer idServicio) {
        if (estaServicioOcultoOEliminado(servicio)) {
            log.info("ServicioCrudBackendDebug 404 por servicio oculto/eliminado idServicio={} oculto={} estadoModeracion={}",
                    idServicio, servicio.getOculto(), servicio.getEstadoModeracion());
            throw new NoSuchElementException("Servicio no encontrado con ID: " + idServicio);
        }
    }

    private boolean estaServicioOcultoOEliminado(Servicio servicio) {
        return servicio == null
                || Boolean.TRUE.equals(servicio.getOculto())
                || servicio.getEstadoModeracion() == EstadoModeracion.OCULTO
                || servicio.getEstadoModeracion() == EstadoModeracion.ELIMINADO_POR_MODERACION;
    }

    private Integer obtenerPropietarioId(Servicio servicio) {
        return servicio != null && servicio.getUsuario() != null ? servicio.getUsuario().getIdUsuario() : null;
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
        servicio.setFechaOculto(com.example.demo.util.ArtistlanDateTimeUtils.nowMexico());
        repo.save(servicio);
        log.info("ServicioCrudBackendDebug soft delete ejecutado idServicio={} motivo={}", servicio.getIdServicio(), motivo);
    }
}
