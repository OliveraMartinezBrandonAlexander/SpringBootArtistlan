package com.example.demo.service.impl;

import com.example.demo.dto.moderacion.CrearReporteRequestDTO;
import com.example.demo.dto.moderacion.ReporteDetalleDTO;
import com.example.demo.dto.moderacion.ReporteResumenDTO;
import com.example.demo.enums.EstadoCuenta;
import com.example.demo.enums.EstadoModeracion;
import com.example.demo.enums.EstadoReporte;
import com.example.demo.enums.PrioridadReporte;
import com.example.demo.enums.TipoObjetivoReporte;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Obra;
import com.example.demo.model.Reporte;
import com.example.demo.model.ResolucionReporte;
import com.example.demo.model.Servicio;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.ReporteRepository;
import com.example.demo.repository.ResolucionReporteRepository;
import com.example.demo.repository.ServicioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.NotificacionService;
import com.example.demo.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private static final String USUARIO_NO_DISPONIBLE = "Usuario no disponible";
    private static final List<EstadoReporte> ESTADOS_REPORTE_ACTIVO = List.of(
            EstadoReporte.PENDIENTE,
            EstadoReporte.EN_REVISION
    );

    private static final List<EstadoModeracion> ESTADOS_CONTENIDO_NO_REPORTABLE = List.of(
            EstadoModeracion.OCULTO,
            EstadoModeracion.ELIMINADO_POR_MODERACION
    );

    private final ReporteRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObraRepository obraRepository;
    private final ServicioRepository servicioRepository;
    private final ResolucionReporteRepository resolucionReporteRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public ReporteDetalleDTO crearReporte(CrearReporteRequestDTO request) {
        validarRequestBase(request);

        Usuario reportante = usuarioRepository.findById(request.getIdUsuarioReportante())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario reportante no encontrado"));

        validarUsuarioReportanteActivo(reportante);
        validarObjetivoExacto(request);

        Reporte.ReporteBuilder builder = Reporte.builder()
                .usuarioReportante(reportante)
                .tipoObjetivo(request.getTipoObjetivo())
                .motivo(normalizarTextoObligatorio(request.getMotivo(), "motivo es obligatorio"))
                .descripcion(normalizarTextoOpcional(request.getDescripcion()))
                .estado(EstadoReporte.PENDIENTE)
                .prioridad(PrioridadReporte.MEDIA);

        switch (request.getTipoObjetivo()) {
            case OBRA -> builder.obra(validarYObtenerObraReportable(request.getIdObra(), reportante));
            case SERVICIO -> builder.servicio(validarYObtenerServicioReportable(request.getIdServicio(), reportante));
            case USUARIO -> builder.usuarioReportado(validarYObtenerUsuarioReportable(request.getIdUsuarioReportado(), reportante));
        }

        Reporte guardado = reporteRepository.save(builder.build());
        crearNotificacionModeradoresSiAplica(guardado);
        return toDetalleDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResumenDTO> listarReportesDeUsuario(Integer idUsuario) {
        validarUsuarioExiste(idUsuario);
        return reporteRepository.findByUsuarioReportante_IdUsuarioOrderByFechaReporteDesc(idUsuario)
                .stream()
                .map(this::toResumenDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteDetalleDTO obtenerDetalleReporteDeUsuario(Integer idUsuario, Integer idReporte) {
        validarUsuarioExiste(idUsuario);

        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));

        if (reporte.getUsuarioReportante() == null
                || reporte.getUsuarioReportante().getIdUsuario() == null
                || !idUsuario.equals(reporte.getUsuarioReportante().getIdUsuario())) {
            throw new ResponseStatusException(FORBIDDEN, "No puedes consultar este reporte");
        }

        return toDetalleDto(reporte);
    }

    private void validarRequestBase(CrearReporteRequestDTO request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "El request de reporte es obligatorio");
        }
        if (request.getIdUsuarioReportante() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "idUsuarioReportante es obligatorio");
        }
        if (request.getTipoObjetivo() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "tipoObjetivo es obligatorio");
        }
    }

    private void validarUsuarioReportanteActivo(Usuario reportante) {
        if (reportante.getEstadoCuenta() != EstadoCuenta.ACTIVO) {
            throw new ResponseStatusException(FORBIDDEN, "El usuario reportante debe estar ACTIVO");
        }
        if (esRolModeracion(reportante)) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Los administradores y moderadores no pueden crear reportes desde el flujo de usuario."
            );
        }
    }

    private boolean esRolModeracion(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(usuario.getRol()) || "MODERADOR".equalsIgnoreCase(usuario.getRol());
    }

    private void validarObjetivoExacto(CrearReporteRequestDTO request) {
        switch (request.getTipoObjetivo()) {
            case OBRA -> {
                if (request.getIdObra() == null || request.getIdServicio() != null || request.getIdUsuarioReportado() != null) {
                    throw new ResponseStatusException(BAD_REQUEST, "Para tipoObjetivo OBRA solo debe enviarse idObra");
                }
            }
            case SERVICIO -> {
                if (request.getIdServicio() == null || request.getIdObra() != null || request.getIdUsuarioReportado() != null) {
                    throw new ResponseStatusException(BAD_REQUEST, "Para tipoObjetivo SERVICIO solo debe enviarse idServicio");
                }
            }
            case USUARIO -> {
                if (request.getIdUsuarioReportado() == null || request.getIdObra() != null || request.getIdServicio() != null) {
                    throw new ResponseStatusException(BAD_REQUEST, "Para tipoObjetivo USUARIO solo debe enviarse idUsuarioReportado");
                }
            }
        }
    }

    private Obra validarYObtenerObraReportable(Integer idObra, Usuario reportante) {
        Obra obra = obraRepository.findById(idObra)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada"));

        Usuario dueno = obra.getUsuario();
        if (dueno != null && dueno.getIdUsuario() != null && dueno.getIdUsuario().equals(reportante.getIdUsuario())) {
            throw new ResponseStatusException(FORBIDDEN, "No puedes reportar tu propia obra");
        }
        if (Boolean.TRUE.equals(obra.getOculta())) {
            throw new BusinessException("La obra no es reportable");
        }
        if (ESTADOS_CONTENIDO_NO_REPORTABLE.contains(obra.getEstadoModeracion())) {
            throw new BusinessException("La obra no es reportable");
        }
        if ("RESERVADA".equalsIgnoreCase(obra.getEstado()) || "VENDIDA".equalsIgnoreCase(obra.getEstado())) {
            throw new BusinessException("La obra no es reportable en su estado comercial actual");
        }
        boolean duplicado = reporteRepository.existsByUsuarioReportante_IdUsuarioAndObra_IdObraAndEstadoIn(
                reportante.getIdUsuario(),
                obra.getIdObra(),
                ESTADOS_REPORTE_ACTIVO
        );
        if (duplicado) {
            throw new BusinessException("Ya existe un reporte activo sobre esta obra");
        }
        return obra;
    }

    private Servicio validarYObtenerServicioReportable(Integer idServicio, Usuario reportante) {
        Servicio servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        Usuario dueno = servicio.getUsuario();
        if (dueno != null && dueno.getIdUsuario() != null && dueno.getIdUsuario().equals(reportante.getIdUsuario())) {
            throw new ResponseStatusException(FORBIDDEN, "No puedes reportar tu propio servicio");
        }
        if (Boolean.TRUE.equals(servicio.getOculto())) {
            throw new BusinessException("El servicio no es reportable");
        }
        if (ESTADOS_CONTENIDO_NO_REPORTABLE.contains(servicio.getEstadoModeracion())) {
            throw new BusinessException("El servicio no es reportable");
        }
        boolean duplicado = reporteRepository.existsByUsuarioReportante_IdUsuarioAndServicio_IdServicioAndEstadoIn(
                reportante.getIdUsuario(),
                servicio.getIdServicio(),
                ESTADOS_REPORTE_ACTIVO
        );
        if (duplicado) {
            throw new BusinessException("Ya existe un reporte activo sobre este servicio");
        }
        return servicio;
    }

    private Usuario validarYObtenerUsuarioReportable(Integer idUsuarioReportado, Usuario reportante) {
        Usuario usuarioReportado = usuarioRepository.findById(idUsuarioReportado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario reportado no encontrado"));

        if (usuarioReportado.getIdUsuario() != null && usuarioReportado.getIdUsuario().equals(reportante.getIdUsuario())) {
            throw new ResponseStatusException(FORBIDDEN, "No puedes reportarte a ti mismo");
        }
        if (usuarioReportado.getEstadoCuenta() == EstadoCuenta.DESACTIVADO
                || usuarioReportado.getEstadoCuenta() == EstadoCuenta.BLOQUEADO_PERMANENTE) {
            throw new BusinessException("El usuario no es reportable");
        }
        boolean duplicado = reporteRepository.existsByUsuarioReportante_IdUsuarioAndUsuarioReportado_IdUsuarioAndEstadoIn(
                reportante.getIdUsuario(),
                usuarioReportado.getIdUsuario(),
                ESTADOS_REPORTE_ACTIVO
        );
        if (duplicado) {
            throw new BusinessException("Ya existe un reporte activo sobre este usuario");
        }
        return usuarioReportado;
    }

    private void crearNotificacionModeradoresSiAplica(Reporte reporte) {
        notificacionService.crearNotificacionModeradoresYAdminsActivos(
                "REPORTE_RECIBIDO",
                "Nuevo reporte recibido",
                obtenerMensajeNotificacionNuevoReporte(reporte),
                "REPORTE",
                reporte.getIdReporte()
        );
    }

    private String obtenerMensajeNotificacionNuevoReporte(Reporte reporte) {
        if (reporte == null || reporte.getTipoObjetivo() == null) {
            return "Se ha recibido un nuevo reporte.";
        }
        return switch (reporte.getTipoObjetivo()) {
            case OBRA -> "Se ha recibido un reporte sobre una obra.";
            case SERVICIO -> "Se ha recibido un reporte sobre un servicio.";
            case USUARIO -> "Se ha recibido un reporte a un usuario.";
        };
    }

    private void validarUsuarioExiste(Integer idUsuario) {
        if (idUsuario == null) {
            throw new ResponseStatusException(BAD_REQUEST, "idUsuario es obligatorio");
        }
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
    }

    private ReporteResumenDTO toResumenDto(Reporte reporte) {
        Usuario usuarioAfectado = obtenerUsuarioAfectado(reporte);
        Usuario moderador = reporte.getModeradorAsignado();

        return ReporteResumenDTO.builder()
                .idReporte(reporte.getIdReporte())
                .tipoObjetivo(reporte.getTipoObjetivo())
                .idObra(reporte.getObra() != null ? reporte.getObra().getIdObra() : null)
                .idServicio(reporte.getServicio() != null ? reporte.getServicio().getIdServicio() : null)
                .idUsuarioReportado(reporte.getUsuarioReportado() != null ? reporte.getUsuarioReportado().getIdUsuario() : null)
                .tituloObjetivo(obtenerTituloObjetivo(reporte))
                .nombreUsuarioReportante(obtenerNombreVisible(reporte.getUsuarioReportante()))
                .nombreUsuarioReportado(obtenerNombreVisible(usuarioAfectado))
                .motivo(reporte.getMotivo())
                .estado(reporte.getEstado())
                .prioridad(reporte.getPrioridad())
                .idModeradorAsignado(moderador != null ? moderador.getIdUsuario() : null)
                .nombreModeradorAsignado(obtenerNombreVisible(moderador))
                .fechaReporte(reporte.getFechaReporte())
                .build();
    }

    private ReporteDetalleDTO toDetalleDto(Reporte reporte) {
        Usuario usuarioAfectado = obtenerUsuarioAfectado(reporte);
        Usuario duenoObjetivo = obtenerDuenoObjetivo(reporte);
        Usuario moderador = reporte.getModeradorAsignado();
        ResolucionReporte resolucion = reporte.getIdReporte() != null
                ? resolucionReporteRepository.findByReporte_IdReporte(reporte.getIdReporte()).orElse(null)
                : null;

        return ReporteDetalleDTO.builder()
                .idReporte(reporte.getIdReporte())
                .tipoObjetivo(reporte.getTipoObjetivo())
                .idObra(reporte.getObra() != null ? reporte.getObra().getIdObra() : null)
                .idServicio(reporte.getServicio() != null ? reporte.getServicio().getIdServicio() : null)
                .idUsuarioReportado(reporte.getUsuarioReportado() != null ? reporte.getUsuarioReportado().getIdUsuario() : null)
                .tituloObjetivo(obtenerTituloObjetivo(reporte))
                .descripcionObjetivo(obtenerDescripcionObjetivo(reporte))
                .imagenObjetivo(obtenerImagenObjetivo(reporte))
                .idUsuarioReportante(reporte.getUsuarioReportante() != null ? reporte.getUsuarioReportante().getIdUsuario() : null)
                .nombreUsuarioReportante(obtenerNombreVisible(reporte.getUsuarioReportante()))
                .idUsuarioDuenoObjetivo(duenoObjetivo != null ? duenoObjetivo.getIdUsuario() : null)
                .nombreUsuarioDuenoObjetivo(obtenerNombreVisible(duenoObjetivo))
                .motivo(reporte.getMotivo())
                .descripcion(reporte.getDescripcion())
                .estado(reporte.getEstado())
                .prioridad(reporte.getPrioridad())
                .idModeradorAsignado(moderador != null ? moderador.getIdUsuario() : null)
                .nombreModeradorAsignado(obtenerNombreVisible(moderador))
                .fechaReporte(reporte.getFechaReporte())
                .fechaInicioRevision(reporte.getFechaInicioRevision())
                .fechaActualizacion(reporte.getFechaActualizacion())
                .estadoModeracionContenido(obtenerEstadoModeracionContenido(reporte))
                .estadoCuentaUsuarioReportado(usuarioAfectado != null ? usuarioAfectado.getEstadoCuenta() : null)
                .accionResolucion(resolucion != null ? resolucion.getAccion() : null)
                .mensajeRespuesta(resolucion != null ? resolucion.getMensajeRespuesta() : null)
                .fechaResolucion(resolucion != null ? resolucion.getFechaResolucion() : null)
                .build();
    }

    private Usuario obtenerUsuarioAfectado(Reporte reporte) {
        if (reporte == null) {
            return null;
        }
        if (reporte.getUsuarioReportado() != null) {
            return reporte.getUsuarioReportado();
        }
        if (reporte.getObra() != null) {
            return reporte.getObra().getUsuario();
        }
        if (reporte.getServicio() != null) {
            return reporte.getServicio().getUsuario();
        }
        return null;
    }

    private Usuario obtenerDuenoObjetivo(Reporte reporte) {
        return obtenerUsuarioAfectado(reporte);
    }

    private String obtenerTituloObjetivo(Reporte reporte) {
        if (reporte == null || reporte.getTipoObjetivo() == null) {
            return null;
        }
        return switch (reporte.getTipoObjetivo()) {
            case OBRA -> reporte.getObra() != null ? reporte.getObra().getTitulo() : null;
            case SERVICIO -> reporte.getServicio() != null ? reporte.getServicio().getTitulo() : null;
            case USUARIO -> obtenerNombreVisible(reporte.getUsuarioReportado());
        };
    }

    private String obtenerDescripcionObjetivo(Reporte reporte) {
        if (reporte == null || reporte.getTipoObjetivo() == null) {
            return null;
        }
        return switch (reporte.getTipoObjetivo()) {
            case OBRA -> reporte.getObra() != null ? reporte.getObra().getDescripcion() : null;
            case SERVICIO -> reporte.getServicio() != null ? reporte.getServicio().getDescripcion() : null;
            case USUARIO -> reporte.getUsuarioReportado() != null ? reporte.getUsuarioReportado().getDescripcion() : null;
        };
    }

    private String obtenerImagenObjetivo(Reporte reporte) {
        if (reporte == null || reporte.getTipoObjetivo() == null) {
            return null;
        }
        return switch (reporte.getTipoObjetivo()) {
            case OBRA -> reporte.getObra() != null ? reporte.getObra().getImagen1() : null;
            case SERVICIO -> null;
            case USUARIO -> reporte.getUsuarioReportado() != null ? reporte.getUsuarioReportado().getFotoPerfil() : null;
        };
    }

    private EstadoModeracion obtenerEstadoModeracionContenido(Reporte reporte) {
        if (reporte == null || reporte.getTipoObjetivo() == null) {
            return null;
        }
        return switch (reporte.getTipoObjetivo()) {
            case OBRA -> reporte.getObra() != null ? reporte.getObra().getEstadoModeracion() : null;
            case SERVICIO -> reporte.getServicio() != null ? reporte.getServicio().getEstadoModeracion() : null;
            case USUARIO -> null;
        };
    }

    private String obtenerNombreVisible(Usuario usuario) {
        if (usuario == null) {
            return USUARIO_NO_DISPONIBLE;
        }
        return primerNoVacio(usuario.getUsuario(), usuario.getNombreCompleto(), usuario.getCorreo(), USUARIO_NO_DISPONIBLE);
    }

    private String primerNoVacio(String... valores) {
        if (valores == null) {
            return null;
        }
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private String normalizarTextoObligatorio(String valor, String mensajeError) {
        String limpio = normalizarTextoOpcional(valor);
        if (limpio == null) {
            throw new ResponseStatusException(BAD_REQUEST, mensajeError);
        }
        return limpio;
    }

    private String normalizarTextoOpcional(String valor) {
        return Optional.ofNullable(valor)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .orElse(null);
    }
}
