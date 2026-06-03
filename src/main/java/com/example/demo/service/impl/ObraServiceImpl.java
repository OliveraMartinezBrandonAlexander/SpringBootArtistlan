package com.example.demo.service.impl;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dto.ObraDTO;
import com.example.demo.enums.EstadoCuenta;
import com.example.demo.enums.EstadoModeracion;
import com.example.demo.enums.TipoMetaPersonal;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.MetaPersonalService;
import com.example.demo.service.NotificacionService;
import com.example.demo.service.ObraService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@AllArgsConstructor
@Slf4j
public class ObraServiceImpl implements ObraService {

    private static final String MENSAJE_OBRA_RETIRADA_MODIFICAR = "Esta obra fue retirada por moderacion y no puede modificarse.";
    private static final String MENSAJE_OBRA_RETIRADA_ELIMINAR = "Esta obra fue retirada por moderacion y no puede eliminarse desde el portafolio.";
    private static final String ESTADO_EN_VENTA = "EN_VENTA";
    private static final String ESTADO_EN_EXHIBICION = "EN_EXHIBICION";
    private static final String ESTADO_RESERVADA = "RESERVADA";
    private static final String ESTADO_VENDIDA = "VENDIDA";
    private static final String ESTADO_TRANSACCION_COMPLETADA = "CAPTURADA";
    private static final int LIMIT_POPULARES_DEFAULT = 10;
    private static final int LIMIT_POPULARES_MAX = 20;
    private static final int CATEGORIA_OBRA_MIN = 1;
    private static final int CATEGORIA_OBRA_MAX = 18;
    private static final List<EstadoModeracion> ESTADOS_NO_VISIBLES_PUBLICO = List.of(
            EstadoModeracion.OCULTO,
            EstadoModeracion.ELIMINADO_POR_MODERACION
    );
    private static final List<EstadoCuenta> ESTADOS_DUENO_NO_VISIBLES_PUBLICO = List.of(
            EstadoCuenta.DESACTIVADO,
            EstadoCuenta.BLOQUEADO_PERMANENTE
    );

    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaObrasRepository categoriaObraRepository;
    private final CategoriaRepository categoriaRepository;
    private final FavoritosRepository favoritosRepository;
    private final CarritoRepository carritoRepository;
    private final CompraObraRepository compraObraRepository;
    private final CompraCarritoDetalleRepository compraCarritoDetalleRepository;
    private final SolicitudCompraObraRepository solicitudRepository;
    private final MetaPersonalService metaPersonalService;
    private final NotificacionService notificacionService;

    @Override
    public Obra guardar(Obra o) {
        Integer idUsuarioAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        Integer idUsuarioSolicitado = o != null && o.getUsuario() != null ? o.getUsuario().getIdUsuario() : null;
        if (idUsuarioSolicitado != null && !idUsuarioSolicitado.equals(idUsuarioAutenticado)) {
            throw new ResponseStatusException(FORBIDDEN, "No puedes publicar una obra para otro usuario.");
        }
        Usuario usuarioAutenticado = usuarioRepository.findById(idUsuarioAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuarioAutenticado));
        o.setUsuario(usuarioAutenticado);
        if (o.getEstado() == null || o.getEstado().isBlank()) {
            o.setEstado(ESTADO_EN_VENTA);
        } else {
            o.setEstado(normalizarEstado(o.getEstado()));
        }
        return obraRepository.save(o);
    }

    @Override
    public List<Obra> listar() {
        return obraRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Obra> listarPublicasVisibles() {
        List<Obra> obras = obraRepository.findPublicasVisibles(
                ESTADOS_NO_VISIBLES_PUBLICO,
                ESTADOS_DUENO_NO_VISIBLES_PUBLICO
        );
        inicializarRelacionesPublicas(obras);
        return obras;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Obra> obtenerObrasPopularesParaCarrusel(Integer limit) {
        int limiteFinal = normalizarLimitePopulares(limit);
        Pageable pageable = PageRequest.of(0, limiteFinal);

        LinkedHashSet<Integer> idsSeleccionados = new LinkedHashSet<>(
                obraRepository.findIdsObrasPopularesVisibles(
                        ESTADOS_NO_VISIBLES_PUBLICO,
                        ESTADOS_DUENO_NO_VISIBLES_PUBLICO,
                        pageable
                )
        );

        int restantes = limiteFinal - idsSeleccionados.size();
        if (restantes > 0) {
            Pageable recentPageable = PageRequest.of(0, restantes);
            List<Integer> idsRecientes = idsSeleccionados.isEmpty()
                    ? obraRepository.findIdsObrasRecientesVisibles(
                    ESTADOS_NO_VISIBLES_PUBLICO,
                    ESTADOS_DUENO_NO_VISIBLES_PUBLICO,
                    recentPageable
            )
                    : obraRepository.findIdsObrasRecientesVisiblesExcluyendo(
                    ESTADOS_NO_VISIBLES_PUBLICO,
                    ESTADOS_DUENO_NO_VISIBLES_PUBLICO,
                    List.copyOf(idsSeleccionados),
                    recentPageable
            );
            idsSeleccionados.addAll(idsRecientes);
        }

        if (idsSeleccionados.isEmpty()) {
            return List.of();
        }

        List<Integer> idsOrdenados = idsSeleccionados.stream()
                .limit(limiteFinal)
                .toList();

        Map<Integer, Obra> obrasPorId = obraRepository.findPublicasVisiblesByIds(
                        idsOrdenados,
                        ESTADOS_NO_VISIBLES_PUBLICO,
                        ESTADOS_DUENO_NO_VISIBLES_PUBLICO
                ).stream()
                .collect(Collectors.toMap(Obra::getIdObra, Function.identity(), (first, second) -> first));

        List<Obra> obras = idsOrdenados.stream()
                .map(obrasPorId::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        inicializarRelacionesPublicas(obras);
        return obras;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Obra> listarPublicasVisiblesPaginado(String q, String categoria, Integer idCategoria, Pageable pageable) {
        Page<Obra> page = obraRepository.findPublicasVisiblesPaginado(
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
    public Optional<Obra> buscarPorId(Integer id) {
        return obraRepository.findByIdConCategoria(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Obra> buscarPublicaVisiblePorId(Integer id) {
        Optional<Obra> obra = obraRepository.findPublicaVisiblePorId(
                id,
                ESTADOS_NO_VISIBLES_PUBLICO,
                ESTADOS_DUENO_NO_VISIBLES_PUBLICO
        );
        obra.ifPresent(this::inicializarRelacionPublica);
        return obra;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Obra> buscarDetalleVisibleOPropioPorId(Integer id, Integer usuarioId) {
        log.info("ObraCrudBackendDebug GET detalle recibido idObra={} usuarioId={}", id, usuarioId);

        Optional<Obra> obraPublica = buscarPublicaVisiblePorId(id);
        if (obraPublica.isPresent()) {
            Obra obra = obraPublica.get();
            Integer propietario = obtenerPropietarioId(obra);
            log.info("ObraCrudBackendDebug GET detalle publico encontrado idObra={} propietario={} usuarioId={}",
                    id, propietario, usuarioId);
            return obraPublica;
        }

        Optional<Obra> obraOpt = obraRepository.findByIdConCategoria(id);
        log.info("ObraCrudBackendDebug GET detalle existe={} idObra={}", obraOpt.isPresent(), id);
        if (obraOpt.isEmpty()) {
            return Optional.empty();
        }

        Obra obra = obraOpt.get();
        Integer propietario = obtenerPropietarioId(obra);
        log.info("ObraCrudBackendDebug GET detalle propietario real idObra={} propietario={} usuarioId={} oculta={} estadoModeracion={}",
                id, propietario, usuarioId, obra.getOculta(), obra.getEstadoModeracion());

        if (estaObraOcultaOEliminada(obra)) {
            log.info("ObraCrudBackendDebug GET detalle 404 por obra oculta/eliminada idObra={}", id);
            return Optional.empty();
        }

        if (usuarioId == null) {
            log.info("ObraCrudBackendDebug GET detalle 404 sin usuarioId para obra no publica idObra={}", id);
            return Optional.empty();
        }

        validarPertenencia(obra, usuarioId);
        inicializarRelacionPublica(obra);
        log.info("ObraCrudBackendDebug GET detalle propio editable OK idObra={} propietario={}", id, propietario);
        return Optional.of(obra);
    }

    @Override
    @Transactional
    public Optional<Obra> actualizarObra(Integer id, ObraDTO obra) {
        Integer idUsuarioAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        return obraRepository.findByIdConCategoria(id).map(existente -> {
            validarNoOcultaOEliminada(existente, id);
            validarPertenencia(existente, idUsuarioAutenticado);
            validarNoRetiradaPorModeracionParaEditar(existente);
            if (obra.getIdUsuario() != null && !idUsuarioAutenticado.equals(obra.getIdUsuario())) {
                throw new ResponseStatusException(FORBIDDEN, "No puedes editar una obra de otro usuario.");
            }
            aplicarActualizacionObra(existente, obra);
            return obraRepository.save(existente);
        });
    }

    @Override
    @Transactional
    public Obra guardarObraConCategoria(Integer usuarioId, ObraDTO obraDTO) {
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(usuarioId);
        if (obraDTO.getIdUsuario() != null && !idUsuarioAutenticado.equals(obraDTO.getIdUsuario())) {
            throw new ResponseStatusException(FORBIDDEN, "No puedes publicar una obra para otro usuario.");
        }
        Usuario usuario = usuarioRepository.findById(idUsuarioAutenticado)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + idUsuarioAutenticado));

        Obra obra = new Obra();
        aplicarCamposCreacion(obra, obraDTO);
        obra.setUsuario(usuario);
        obra.setFechaPublicacion(com.example.demo.util.ArtistlanDateTimeUtils.nowMexico());

        Obra obraGuardada = obraRepository.save(obra);
        reemplazarCategoria(obraGuardada, obraDTO.getIdCategoria());
        metaPersonalService.evaluarMetasDelUsuarioPorTipos(idUsuarioAutenticado, EnumSet.of(TipoMetaPersonal.PUBLICACIONES));

        return obraRepository.findByIdConCategoria(obraGuardada.getIdObra()).orElse(obraGuardada);
    }

    @Override
    @Transactional
    public Obra actualizarObraDeUsuario(Integer usuarioId, Integer obraId, ObraDTO obraDTO) {
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(usuarioId);
        log.info("ObraCrudBackendDebug UPDATE recibido usuarioId={} idObra={}", idUsuarioAutenticado, obraId);
        Obra obraExistente = obraRepository.findByIdConCategoria(obraId)
                .orElseThrow(() -> new NoSuchElementException("Obra no encontrada con ID: " + obraId));

        log.info("ObraCrudBackendDebug UPDATE existe=true idObra={} propietario={} usuarioId={} oculta={} estadoModeracion={}",
                obraId, obtenerPropietarioId(obraExistente), idUsuarioAutenticado, obraExistente.getOculta(), obraExistente.getEstadoModeracion());
        validarNoOcultaOEliminada(obraExistente, obraId);
        validarPertenencia(obraExistente, idUsuarioAutenticado);
        log.info("ObraCrudBackendDebug UPDATE validacion propietario OK idObra={} usuarioId={}", obraId, idUsuarioAutenticado);
        validarNoRetiradaPorModeracionParaEditar(obraExistente);
        aplicarActualizacionObra(obraExistente, obraDTO);

        Obra guardada = obraRepository.save(obraExistente);
        log.info("ObraCrudBackendDebug UPDATE guardada idObra={} usuarioId={}", guardada.getIdObra(), idUsuarioAutenticado);
        return obraRepository.findByIdConCategoria(guardada.getIdObra()).orElse(guardada);
    }

    @Override
    @Transactional
    public void eliminarObraDeUsuario(Integer usuarioId, Integer obraId) {
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(usuarioId);
        log.info("ObraCrudBackendDebug DELETE recibido usuarioId={} idObra={}", idUsuarioAutenticado, obraId);
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada con ID: " + obraId));

        log.info("ObraCrudBackendDebug DELETE existe=true idObra={} propietario={} usuarioId={} oculta={} estadoModeracion={}",
                obraId, obtenerPropietarioId(obra), idUsuarioAutenticado, obra.getOculta(), obra.getEstadoModeracion());
        validarNoOcultaOEliminada(obra, obraId);
        validarPertenencia(obra, idUsuarioAutenticado);
        log.info("ObraCrudBackendDebug DELETE validacion propietario OK idObra={} usuarioId={}", obraId, idUsuarioAutenticado);
        validarNoRetiradaPorModeracionParaEliminar(obra);
        validarEstadoParaEliminar(obra);
        cancelarSolicitudesActivasDeObra(
                obraId,
                "La obra fue eliminada por el artista.",
                "Tu solicitud para '%s' fue cancelada porque la obra fue eliminada por el artista.",
                true
        );
        ocultarObraLogicamente(obra, "Obra eliminada por el usuario");
        int activasDespues = obraRepository.findPropiasActivasByUsuarioId(idUsuarioAutenticado, ESTADOS_NO_VISIBLES_PUBLICO).size();
        log.info("ObraCrudBackendDebug DELETE soft delete OK idObra={} usuarioId={} obrasActivasDespues={}",
                obraId, idUsuarioAutenticado, activasDespues);
    }

    @Override
    @Transactional
    public boolean eliminar(Integer id) {
        Integer idUsuarioAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        Obra obra = obraRepository.findById(id).orElse(null);
        if (obra == null) {
            return false;
        }
        if (estaObraOcultaOEliminada(obra)) {
            log.info("ObraCrudBackendDebug DELETE /api/obras/{} no ejecutado: ya oculta/eliminada", id);
            return false;
        }
        validarPertenencia(obra, idUsuarioAutenticado);
        validarNoRetiradaPorModeracionParaEliminar(obra);
        validarEstadoParaEliminar(obra);
        cancelarSolicitudesActivasDeObra(
                id,
                "La obra fue eliminada por el artista.",
                "Tu solicitud para '%s' fue cancelada porque la obra fue eliminada por el artista.",
                true
        );
        ocultarObraLogicamente(obra, "Obra eliminada por el usuario");
        return true;
    }

    @Override
    public Optional<Obra> actualizarImagen1(Integer id, String urlImagen) {
        Integer idUsuarioAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        return obraRepository.findById(id).map(o -> {
            validarNoOcultaOEliminada(o, id);
            validarPertenencia(o, idUsuarioAutenticado);
            validarNoRetiradaPorModeracionParaEditar(o);
            o.setImagen1(urlImagen);
            return obraRepository.save(o);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Obra> buscarPorUsuarioId(Integer usuarioId) {
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(usuarioId);
        List<Obra> obras = obraRepository.findPropiasActivasByUsuarioId(
                idUsuarioAutenticado,
                ESTADOS_NO_VISIBLES_PUBLICO
        );
        log.info("PortafolioBackendDebug listado obras propias usuarioId={} totalActivas={}", idUsuarioAutenticado, obras.size());
        return obras;
    }

    @Override
    @Transactional
    public void eliminarPorUsuarioId(Integer usuarioId) {
        List<Obra> obras = obraRepository.findByUsuarioIdUsuario(usuarioId);
        for (Obra obra : obras) {
            String estado = normalizarEstado(obra.getEstado());
            if (!ESTADO_RESERVADA.equals(estado) && !ESTADO_VENDIDA.equals(estado)) {
                ocultarObraLogicamente(obra, "Obra ocultada por desactivacion de cuenta");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaDisponibleParaVenta(Integer idObra) {
        Obra obra = obraRepository.findById(idObra)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada con ID: " + idObra));
        return ESTADO_EN_VENTA.equals(normalizarEstado(obra.getEstado()));
    }

    @Override
    @Transactional
    public Obra marcarComoVendida(Integer idObra) {
        Obra obra = obraRepository.findById(idObra)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada con ID: " + idObra));
        obra.setEstado(ESTADO_VENDIDA);
        return obraRepository.save(obra);
    }

    private void aplicarCamposCreacion(Obra destino, ObraDTO origen) {
        destino.setTitulo(origen.getTitulo());
        destino.setDescripcion(origen.getDescripcion());
        destino.setEstado(origen.getEstado() != null ? normalizarEstado(origen.getEstado()) : ESTADO_EN_VENTA);
        destino.setPrecio(origen.getPrecio());
        destino.setImagen1(origen.getImagen1());
        destino.setImagen2(origen.getImagen2());
        destino.setImagen3(origen.getImagen3());
        destino.setTecnicas(origen.getTecnicas());
        destino.setMedidas(origen.getMedidas());
        destino.setConfirmacionAutoria(Boolean.TRUE.equals(origen.getConfirmacionAutoria()));
    }

    private void aplicarActualizacionObra(Obra existente, ObraDTO origen) {
        validarEstadoParaEdicion(existente);

        if (origen.getTitulo() != null) {
            existente.setTitulo(origen.getTitulo());
        }
        if (origen.getDescripcion() != null) {
            existente.setDescripcion(origen.getDescripcion());
        }
        if (origen.getImagen1() != null) {
            existente.setImagen1(origen.getImagen1());
        }
        if (origen.getImagen2() != null) {
            existente.setImagen2(origen.getImagen2());
        }
        if (origen.getImagen3() != null) {
            existente.setImagen3(origen.getImagen3());
        }
        if (origen.getTecnicas() != null) {
            existente.setTecnicas(origen.getTecnicas());
        }
        if (origen.getMedidas() != null) {
            existente.setMedidas(origen.getMedidas());
        }
        if (origen.getConfirmacionAutoria() != null) {
            existente.setConfirmacionAutoria(origen.getConfirmacionAutoria());
        }

        String estadoActual = normalizarEstado(existente.getEstado());
        String estadoObjetivo = estadoActual;
        if (origen.getEstado() != null && !origen.getEstado().isBlank()
                && !ESTADO_VENDIDA.equals(estadoActual)
                && !ESTADO_RESERVADA.equals(estadoActual)) {
            estadoObjetivo = normalizarEstado(origen.getEstado());
            existente.setEstado(estadoObjetivo);
        }

        if (origen.getPrecio() != null
                && existente.getPrecio() == null
                && ESTADO_EN_VENTA.equals(estadoObjetivo)
                && (ESTADO_EN_EXHIBICION.equals(estadoActual) || ESTADO_EN_VENTA.equals(estadoActual))) {
            existente.setPrecio(origen.getPrecio());
        }

        if (ESTADO_EN_VENTA.equals(estadoActual) && ESTADO_EN_EXHIBICION.equals(estadoObjetivo)) {
            cancelarSolicitudesActivasDeObra(
                    existente.getIdObra(),
                    "La obra pas\u00F3 a En exhibici\u00F3n",
                    "Tu solicitud para '%s' fue cancelada porque la obra pas\u00F3 a En exhibici\u00F3n.",
                    false);
        }

        if (origen.getIdCategoria() != null && origen.getIdCategoria() > 0) {
            reemplazarCategoria(existente, origen.getIdCategoria());
        }
    }

    private void reemplazarCategoria(Obra obra, Integer idCategoria) {
        if (idCategoria == null) {
            return;
        }

        Integer categoriaActual = obra.getCategoriaObras().stream()
                .map(CategoriaObras::getCategoria)
                .filter(java.util.Objects::nonNull)
                .map(Categoria::getIdCategoria)
                .findFirst()
                .orElse(null);
        if (java.util.Objects.equals(categoriaActual, idCategoria)) {
            return;
        }

        obra.getCategoriaObras().clear();

        if (idCategoria <= 0) {
            return;
        }
        validarCategoriaObra(idCategoria);

        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new NoSuchElementException("Categoria no encontrada con ID: " + idCategoria));

        CategoriaObras categoriaObra = new CategoriaObras(
                new CategoriaObrasID(obra.getIdObra(), categoria.getIdCategoria()),
                obra,
                categoria
        );

        obra.getCategoriaObras().add(categoriaObra);
    }

    private void validarPertenencia(Obra obra, Integer usuarioId) {
        Integer propietario = obtenerPropietarioId(obra);
        boolean esPropietario = usuarioId != null && usuarioId.equals(propietario);
        log.info("ObraCrudBackendDebug validacion propietario idObra={} usuarioId={} propietario={} resultado={}",
                obra != null ? obra.getIdObra() : null, usuarioId, propietario, esPropietario);
        if (!esPropietario) {
            throw new ResponseStatusException(FORBIDDEN, "La obra no pertenece al usuario indicado");
        }
    }

    private void validarNoOcultaOEliminada(Obra obra, Integer obraId) {
        if (estaObraOcultaOEliminada(obra)) {
            log.info("ObraCrudBackendDebug 404 por obra oculta/eliminada idObra={} oculta={} estadoModeracion={}",
                    obraId, obra.getOculta(), obra.getEstadoModeracion());
            throw new ResourceNotFoundException("Obra no encontrada con ID: " + obraId);
        }
    }

    private boolean estaObraOcultaOEliminada(Obra obra) {
        return obra == null
                || Boolean.TRUE.equals(obra.getOculta())
                || obra.getEstadoModeracion() == EstadoModeracion.OCULTO
                || obra.getEstadoModeracion() == EstadoModeracion.ELIMINADO_POR_MODERACION;
    }

    private Integer obtenerPropietarioId(Obra obra) {
        return obra != null && obra.getUsuario() != null ? obra.getUsuario().getIdUsuario() : null;
    }

    private void validarEstadoParaEdicion(Obra obra) {
        String estado = normalizarEstado(obra.getEstado());
        if (ESTADO_RESERVADA.equals(estado) || ESTADO_VENDIDA.equals(estado)) {
            throw new BusinessException("La obra no puede editarse en estado " + obra.getEstado());
        }
    }

    private void validarEstadoParaEliminar(Obra obra) {
        String estado = normalizarEstado(obra.getEstado());
        if (ESTADO_RESERVADA.equals(estado) || ESTADO_VENDIDA.equals(estado)) {
            throw new BusinessException("La obra no puede eliminarse en estado " + obra.getEstado());
        }
    }

    private void validarNoRetiradaPorModeracionParaEditar(Obra obra) {
        if (obra != null && obra.getEstadoModeracion() == EstadoModeracion.ELIMINADO_POR_MODERACION) {
            throw new ResponseStatusException(CONFLICT, MENSAJE_OBRA_RETIRADA_MODIFICAR);
        }
    }

    private void validarNoRetiradaPorModeracionParaEliminar(Obra obra) {
        if (obra != null && obra.getEstadoModeracion() == EstadoModeracion.ELIMINADO_POR_MODERACION) {
            throw new ResponseStatusException(CONFLICT, MENSAJE_OBRA_RETIRADA_ELIMINAR);
        }
    }

    private String normalizarEstado(String estado) {
        if (estado == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(estado, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private int normalizarLimitePopulares(Integer limit) {
        if (limit == null || limit <= 0) {
            return LIMIT_POPULARES_DEFAULT;
        }
        return Math.min(limit, LIMIT_POPULARES_MAX);
    }

    private void validarCategoriaObra(Integer idCategoria) {
        if (idCategoria < CATEGORIA_OBRA_MIN || idCategoria > CATEGORIA_OBRA_MAX) {
            throw new BusinessException("La categoria de obra debe estar entre 1 y 18.");
        }
    }

    private void cancelarSolicitudesActivasDeObra(Integer obraId, String motivo, String mensajeNotificacion, boolean incluirReferenciaSolicitud) {
        cancelarSolicitudesActivasDeObra(
                obraId,
                motivo,
                mensajeNotificacion,
                incluirReferenciaSolicitud,
                "SOLICITUD_CANCELADA",
                "Solicitud cancelada"
        );
    }

    private void cancelarSolicitudesActivasDeObra(Integer obraId,
                                                  String motivo,
                                                  String mensajeNotificacion,
                                                  boolean incluirReferenciaSolicitud,
                                                  String tipoNotificacion,
                                                  String tituloNotificacion) {
        List<SolicitudCompraObra> activas = solicitudRepository.findByObraIdObraAndEstadoSolicitudIn(
                obraId,
                List.of("PENDIENTE", "ACEPTADA")
        );
        if (activas.isEmpty()) {
            return;
        }

        LocalDateTime ahora = com.example.demo.util.ArtistlanDateTimeUtils.nowMexico();
        for (SolicitudCompraObra solicitud : activas) {
            solicitud.setEstadoSolicitud("CANCELADA");
            solicitud.setFechaRespuesta(ahora);
            solicitud.setFechaExpiracionReserva(null);
            solicitud.setMotivoRechazo(motivo);
        }

        solicitudRepository.saveAll(activas);
        carritoRepository.eliminarTodosPorObra(obraId);

        for (SolicitudCompraObra solicitud : activas) {
            String tituloObra = solicitud.getObra() != null ? solicitud.getObra().getTitulo() : "la obra";
            notificacionService.crearNotificacionSistema(
                    solicitud.getComprador().getIdUsuario(),
                    tipoNotificacion,
                    tituloNotificacion,
                    String.format(mensajeNotificacion, tituloObra),
                    incluirReferenciaSolicitud ? "SOLICITUD" : null,
                    incluirReferenciaSolicitud ? solicitud.getIdSolicitud() : null
            );
        }
    }

    private void validarSinVentaReal(Integer obraId) {
        boolean ventaDirectaReal = compraObraRepository.existsByObraIdObraAndEstado(obraId, ESTADO_TRANSACCION_COMPLETADA);
        boolean ventaCarritoReal = compraCarritoDetalleRepository.existsByObraIdObraAndCompraCarritoEstado(obraId, ESTADO_TRANSACCION_COMPLETADA);
        if (ventaDirectaReal || ventaCarritoReal) {
            throw new BusinessException("No se puede eliminar la obra porque tiene ventas reales registradas.");
        }
    }

    private void limpiarReferenciasCompraNoVendida(Integer obraId) {
        compraCarritoDetalleRepository.deleteNoVendidasByObraId(obraId, ESTADO_TRANSACCION_COMPLETADA);
        compraObraRepository.deleteNoVendidasByObraId(obraId, ESTADO_TRANSACCION_COMPLETADA);
    }

    private void eliminarSolicitudesDeObra(Integer obraId) {
        solicitudRepository.deleteByObraIdObra(obraId);
    }

    private void inicializarRelacionesPublicas(List<Obra> obras) {
        for (Obra obra : obras) {
            inicializarRelacionPublica(obra);
        }
    }

    private void inicializarRelacionPublica(Obra obra) {
        if (obra == null) {
            return;
        }
        if (obra.getUsuario() != null) {
            obra.getUsuario().getIdUsuario();
            obra.getUsuario().getUsuario();
            obra.getUsuario().getFotoPerfil();
        }
        if (obra.getCategoriaObras() != null) {
            obra.getCategoriaObras().size();
        }
    }

    private void ocultarObraLogicamente(Obra obra, String motivo) {
        obra.setOculta(Boolean.TRUE);
        obra.setEstadoModeracion(EstadoModeracion.OCULTO);
        obra.setMotivoOculta(motivo);
        obra.setFechaOculta(com.example.demo.util.ArtistlanDateTimeUtils.nowMexico());
        obraRepository.save(obra);
        log.info("ObraCrudBackendDebug soft delete ejecutado idObra={} motivo={}", obra.getIdObra(), motivo);
    }
}
