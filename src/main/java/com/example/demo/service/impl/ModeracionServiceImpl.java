package com.example.demo.service.impl;

import com.example.demo.dto.moderacion.ReporteDetalleDTO;
import com.example.demo.dto.moderacion.ReporteResumenDTO;
import com.example.demo.dto.moderacion.ResolverReporteRequestDTO;
import com.example.demo.dto.moderacion.RespuestaModeracionDTO;
import com.example.demo.dto.moderacion.TomarReporteRequestDTO;
import com.example.demo.enums.AccionResolucionReporte;
import com.example.demo.enums.AccionModeracionContenido;
import com.example.demo.enums.EstadoCuenta;
import com.example.demo.enums.EstadoModeracion;
import com.example.demo.enums.EstadoReporte;
import com.example.demo.enums.EstadoSuspensionUsuario;
import com.example.demo.enums.PrioridadReporte;
import com.example.demo.enums.TipoContenidoModerado;
import com.example.demo.enums.TipoObjetivoReporte;
import com.example.demo.enums.TipoSancionUsuario;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.ModeracionContenido;
import com.example.demo.model.Obra;
import com.example.demo.model.Reporte;
import com.example.demo.model.ResolucionReporte;
import com.example.demo.model.Servicio;
import com.example.demo.model.SuspensionUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ModeracionContenidoRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.ReporteRepository;
import com.example.demo.repository.ResolucionReporteRepository;
import com.example.demo.repository.ServicioRepository;
import com.example.demo.repository.SuspensionUsuarioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ModeracionService;
import com.example.demo.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@RequiredArgsConstructor
public class ModeracionServiceImpl implements ModeracionService {

    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_MODERADOR = "MODERADOR";
    private static final String USUARIO_NO_DISPONIBLE = "Usuario no disponible";
    private static final EnumSet<EstadoModeracion> ESTADOS_REACTIVABLES_CONTENIDO = EnumSet.of(
            EstadoModeracion.OCULTO,
            EstadoModeracion.ELIMINADO_POR_MODERACION
    );

    private final ReporteRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObraRepository obraRepository;
    private final ServicioRepository servicioRepository;
    private final ResolucionReporteRepository resolucionReporteRepository;
    private final ModeracionContenidoRepository moderacionContenidoRepository;
    private final SuspensionUsuarioRepository suspensionUsuarioRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResumenDTO> listarReportes(Integer idModeradorSolicitante,
                                                  EstadoReporte estado,
                                                  PrioridadReporte prioridad,
                                                  TipoObjetivoReporte tipoObjetivo,
                                                  Boolean soloMios) {
        Usuario moderador = validarModeradorActivo(idModeradorSolicitante);

        List<Reporte> reportes;
        if (Boolean.TRUE.equals(soloMios)) {
            if (estado != null) {
                reportes = reporteRepository.findByEstadoAndModeradorAsignado_IdUsuarioOrderByFechaReporteDesc(
                        estado,
                        moderador.getIdUsuario()
                );
            } else {
                reportes = reporteRepository.findByModeradorAsignado_IdUsuarioOrderByFechaReporteDesc(moderador.getIdUsuario());
            }
        } else {
            if (estado != null) {
                reportes = reporteRepository.findByEstadoOrderByFechaReporteDesc(estado);
            } else {
                reportes = reporteRepository.findAllByOrderByFechaReporteDesc();
            }
        }

        return reportes.stream()
                .filter(reporte -> prioridad == null || prioridad == reporte.getPrioridad())
                .filter(reporte -> tipoObjetivo == null || tipoObjetivo == reporte.getTipoObjetivo())
                .map(this::toResumenDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteDetalleDTO obtenerDetalleReporte(Integer idModeradorSolicitante, Integer idReporte) {
        validarModeradorActivo(idModeradorSolicitante);

        if (idReporte == null) {
            throw new ResponseStatusException(BAD_REQUEST, "idReporte es obligatorio");
        }

        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));

        return toDetalleDto(reporte);
    }

    @Override
    @Transactional
    public RespuestaModeracionDTO tomarReporte(Integer idReporte, TomarReporteRequestDTO request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "El request para tomar reporte es obligatorio");
        }
        if (request.getIdModerador() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "idModerador es obligatorio");
        }
        if (idReporte == null) {
            throw new ResponseStatusException(BAD_REQUEST, "idReporte es obligatorio");
        }

        Usuario moderador = validarModeradorActivo(request.getIdModerador());
        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));

        if (reporte.getEstado() != EstadoReporte.PENDIENTE) {
            throw new BusinessException("El reporte no esta disponible para ser tomado");
        }
        if (reporte.getModeradorAsignado() != null) {
            throw new BusinessException("El reporte ya fue asignado a un moderador");
        }

        validarNoEsPropioContenido(moderador, reporte);

        reporte.setModeradorAsignado(moderador);
        reporte.setEstado(EstadoReporte.EN_REVISION);
        reporte.setFechaInicioRevision(LocalDateTime.now());

        if (request.getPrioridad() != null) {
            reporte.setPrioridad(request.getPrioridad());
        }

        Reporte guardado = reporteRepository.save(reporte);

        return RespuestaModeracionDTO.builder()
                .success(Boolean.TRUE)
                .message("Reporte tomado correctamente para revision")
                .idReporte(guardado.getIdReporte())
                .estadoReporte(guardado.getEstado())
                .accionEjecutada((AccionResolucionReporte) null)
                .estadoModeracionContenido(obtenerEstadoModeracionContenido(guardado))
                .estadoCuentaUsuario(obtenerUsuarioAfectado(guardado) != null
                        ? obtenerUsuarioAfectado(guardado).getEstadoCuenta()
                        : null)
                .fecha(guardado.getFechaActualizacion())
                .build();
    }

    @Override
    @Transactional
    public RespuestaModeracionDTO resolverReporte(Integer idReporte, ResolverReporteRequestDTO request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "El request para resolver reporte es obligatorio");
        }
        if (idReporte == null) {
            throw new ResponseStatusException(BAD_REQUEST, "idReporte es obligatorio");
        }
        if (request.getIdModerador() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "idModerador es obligatorio");
        }
        if (request.getAccion() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "accion es obligatoria");
        }
        if (request.getAccion() == AccionResolucionReporte.SIN_ACCION) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes seleccionar una accion valida para resolver el reporte");
        }
        if (request.getTipoRespuesta() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "tipoRespuesta es obligatoria");
        }

        String mensajeRespuesta = normalizarTextoObligatorio(
                request.getMensajeRespuesta(),
                "mensajeRespuesta es obligatorio"
        );
        String motivoAccion = accionRequiereMotivo(request.getAccion())
                ? normalizarTextoObligatorio(request.getMotivoAccion(), "motivoAccion es obligatorio para esta accion")
                : normalizarTextoOpcional(request.getMotivoAccion());

        Usuario moderador = validarModeradorActivo(request.getIdModerador());
        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));

        validarReporteResoluble(reporte, moderador);

        if (resolucionReporteRepository.existsByReporte_IdReporte(idReporte)) {
            throw new BusinessException("El reporte ya cuenta con una resolucion registrada");
        }

        ejecutarAccionResolucion(
                reporte,
                moderador,
                request.getAccion(),
                motivoAccion,
                request.getFechaFinSuspension(),
                mensajeRespuesta
        );

        reporte.setEstado(request.getAccion() == AccionResolucionReporte.DESCARTAR_REPORTE
                ? EstadoReporte.DESCARTADO
                : EstadoReporte.RESUELTO);

        Reporte reporteGuardado = reporteRepository.save(reporte);

        ResolucionReporte resolucion = resolucionReporteRepository.save(ResolucionReporte.builder()
                .reporte(reporteGuardado)
                .moderador(moderador)
                .mensajeRespuesta(mensajeRespuesta)
                .tipoRespuesta(request.getTipoRespuesta())
                .accion(request.getAccion())
                .build());

        notificarReportanteSegunResolucion(reporteGuardado, request.getAccion(), mensajeRespuesta);

        return RespuestaModeracionDTO.builder()
                .success(Boolean.TRUE)
                .message(obtenerMensajeResolucion(request.getAccion()))
                .idReporte(reporteGuardado.getIdReporte())
                .estadoReporte(reporteGuardado.getEstado())
                .accionEjecutada(request.getAccion())
                .estadoModeracionContenido(obtenerEstadoModeracionContenido(reporteGuardado))
                .estadoCuentaUsuario(obtenerUsuarioAfectado(reporteGuardado) != null
                        ? obtenerUsuarioAfectado(reporteGuardado).getEstadoCuenta()
                        : null)
                .fecha(resolucion.getFechaResolucion())
                .build();
    }

    private Usuario validarModeradorActivo(Integer idModerador) {
        if (idModerador == null) {
            throw new ResponseStatusException(BAD_REQUEST, "idModeradorSolicitante es obligatorio");
        }

        Usuario usuario = usuarioRepository.findById(idModerador)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario moderador no encontrado"));

        if (!esRolModeracion(usuario)) {
            throw new ResponseStatusException(FORBIDDEN, "El usuario no tiene permisos de moderacion");
        }
        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVO) {
            throw new ResponseStatusException(FORBIDDEN, "El usuario moderador debe estar ACTIVO");
        }

        return usuario;
    }

    private boolean esRolModeracion(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            return false;
        }
        return ROL_ADMIN.equals(usuario.getRol()) || ROL_MODERADOR.equals(usuario.getRol());
    }

    private void validarNoEsPropioContenido(Usuario moderador, Reporte reporte) {
        if (moderador == null || moderador.getIdUsuario() == null || reporte == null || reporte.getTipoObjetivo() == null) {
            return;
        }

        Integer idModerador = moderador.getIdUsuario();

        switch (reporte.getTipoObjetivo()) {
            case OBRA -> {
                if (reporte.getObra() != null
                        && reporte.getObra().getUsuario() != null
                        && idModerador.equals(reporte.getObra().getUsuario().getIdUsuario())) {
                    throw new ResponseStatusException(FORBIDDEN, "No puedes moderar un reporte sobre tu propia obra");
                }
            }
            case SERVICIO -> {
                if (reporte.getServicio() != null
                        && reporte.getServicio().getUsuario() != null
                        && idModerador.equals(reporte.getServicio().getUsuario().getIdUsuario())) {
                    throw new ResponseStatusException(FORBIDDEN, "No puedes moderar un reporte sobre tu propio servicio");
                }
            }
            case USUARIO -> {
                if (reporte.getUsuarioReportado() != null
                        && idModerador.equals(reporte.getUsuarioReportado().getIdUsuario())) {
                    throw new ResponseStatusException(FORBIDDEN, "No puedes moderar un reporte sobre tu propio perfil");
                }
            }
        }
    }

    private void validarReporteResoluble(Reporte reporte, Usuario moderador) {
        if (reporte.getEstado() != EstadoReporte.EN_REVISION) {
            throw new BusinessException("El reporte no se encuentra en revision");
        }
        if (reporte.getModeradorAsignado() == null || reporte.getModeradorAsignado().getIdUsuario() == null) {
            throw new ResponseStatusException(FORBIDDEN, "El reporte no tiene moderador asignado");
        }
        if (!reporte.getModeradorAsignado().getIdUsuario().equals(moderador.getIdUsuario())) {
            throw new ResponseStatusException(FORBIDDEN, "Solo el moderador asignado puede resolver este reporte");
        }
        validarNoEsPropioContenido(moderador, reporte);
    }

    private boolean accionRequiereMotivo(AccionResolucionReporte accion) {
        return accion != null && accion != AccionResolucionReporte.DESCARTAR_REPORTE;
    }

    private void ejecutarAccionResolucion(Reporte reporte,
                                          Usuario moderador,
                                          AccionResolucionReporte accion,
                                          String motivoAccion,
                                          LocalDateTime fechaFinSuspension,
                                          String mensajeRespuesta) {
        switch (accion) {
            case DESCARTAR_REPORTE -> {
            }
            case OCULTAR_CONTENIDO -> ocultarContenido(reporte, moderador, motivoAccion, mensajeRespuesta);
            case REACTIVAR_CONTENIDO -> reactivarContenido(reporte, moderador, motivoAccion, mensajeRespuesta);
            case ELIMINAR_CONTENIDO_LOGICO -> eliminarContenidoLogicamente(reporte, moderador, motivoAccion, mensajeRespuesta);
            case ADVERTENCIA -> advertirUsuarioAfectado(reporte, moderador, motivoAccion, mensajeRespuesta);
            case SUSPENDER_USUARIO -> suspenderUsuarioAfectado(reporte, moderador, motivoAccion, fechaFinSuspension, mensajeRespuesta);
            case REACTIVAR_USUARIO -> reactivarUsuarioAfectado(reporte, moderador, motivoAccion, mensajeRespuesta);
            case BLOQUEAR_USUARIO_PERMANENTE -> bloquearUsuarioAfectado(reporte, moderador, motivoAccion, mensajeRespuesta);
            case SIN_ACCION -> throw new ResponseStatusException(BAD_REQUEST, "Debes seleccionar una accion valida para resolver el reporte");
        }
    }

    private void ocultarContenido(Reporte reporte, Usuario moderador, String motivoAccion, String mensajeRespuesta) {
        LocalDateTime ahora = LocalDateTime.now();

        switch (reporte.getTipoObjetivo()) {
            case OBRA -> {
                Obra obra = obtenerObraObjetivo(reporte);
                obra.setOculta(Boolean.TRUE);
                obra.setEstadoModeracion(EstadoModeracion.OCULTO);
                obra.setMotivoOculta(motivoAccion);
                obra.setFechaOculta(ahora);
                obraRepository.save(obra);

                moderacionContenidoRepository.save(ModeracionContenido.builder()
                        .reporte(reporte)
                        .tipoContenido(TipoContenidoModerado.OBRA)
                        .obra(obra)
                        .moderador(moderador)
                        .motivo(motivoAccion)
                        .accion(AccionModeracionContenido.OCULTADO)
                        .build());

                notificarDuenoContenido(
                        obra.getUsuario(),
                        "CONTENIDO_OCULTO",
                        "Tu obra fue ocultada",
                        agregarRespuestaModerador(
                                obtenerReferenciaObraNotificacion(obra) + " fue ocultada.",
                                mensajeRespuesta
                        ),
                        "OBRA",
                        obra.getIdObra()
                );
            }
            case SERVICIO -> {
                Servicio servicio = obtenerServicioObjetivo(reporte);
                servicio.setOculto(Boolean.TRUE);
                servicio.setEstadoModeracion(EstadoModeracion.OCULTO);
                servicio.setMotivoOculto(motivoAccion);
                servicio.setFechaOculto(ahora);
                servicioRepository.save(servicio);

                moderacionContenidoRepository.save(ModeracionContenido.builder()
                        .reporte(reporte)
                        .tipoContenido(TipoContenidoModerado.SERVICIO)
                        .servicio(servicio)
                        .moderador(moderador)
                        .motivo(motivoAccion)
                        .accion(AccionModeracionContenido.OCULTADO)
                        .build());

                notificarDuenoContenido(
                        servicio.getUsuario(),
                        "CONTENIDO_OCULTO",
                        "Tu servicio fue ocultado",
                        agregarRespuestaModerador(
                                obtenerReferenciaServicioNotificacion(servicio) + " fue ocultado.",
                                mensajeRespuesta
                        ),
                        "SERVICIO",
                        servicio.getIdServicio()
                );
            }
            case USUARIO -> throw new BusinessException("La accion OCULTAR_CONTENIDO no aplica a reportes de usuario");
        }
    }

    private void reactivarContenido(Reporte reporte, Usuario moderador, String motivoAccion, String mensajeRespuesta) {
        switch (reporte.getTipoObjetivo()) {
            case OBRA -> {
                Obra obra = obtenerObraObjetivo(reporte);
                boolean reactivable = Boolean.TRUE.equals(obra.getOculta())
                        || ESTADOS_REACTIVABLES_CONTENIDO.contains(obra.getEstadoModeracion());
                if (!reactivable) {
                    throw new BusinessException("La obra no se encuentra en un estado reactivable");
                }

                obra.setOculta(Boolean.FALSE);
                obra.setEstadoModeracion(EstadoModeracion.APROBADO);
                obra.setMotivoOculta(null);
                obra.setFechaOculta(null);
                obraRepository.save(obra);

                moderacionContenidoRepository.save(ModeracionContenido.builder()
                        .reporte(reporte)
                        .tipoContenido(TipoContenidoModerado.OBRA)
                        .obra(obra)
                        .moderador(moderador)
                        .motivo(motivoAccion)
                        .accion(AccionModeracionContenido.REACTIVADO)
                        .build());

                notificarDuenoContenido(
                        obra.getUsuario(),
                        "CONTENIDO_REACTIVADO",
                        "Tu obra fue reactivada",
                        agregarRespuestaModerador(
                                obtenerReferenciaObraNotificacion(obra) + " fue reactivada.",
                                mensajeRespuesta
                        ),
                        "OBRA",
                        obra.getIdObra()
                );
            }
            case SERVICIO -> {
                Servicio servicio = obtenerServicioObjetivo(reporte);
                boolean reactivable = Boolean.TRUE.equals(servicio.getOculto())
                        || ESTADOS_REACTIVABLES_CONTENIDO.contains(servicio.getEstadoModeracion());
                if (!reactivable) {
                    throw new BusinessException("El servicio no se encuentra en un estado reactivable");
                }

                servicio.setOculto(Boolean.FALSE);
                servicio.setEstadoModeracion(EstadoModeracion.APROBADO);
                servicio.setMotivoOculto(null);
                servicio.setFechaOculto(null);
                servicioRepository.save(servicio);

                moderacionContenidoRepository.save(ModeracionContenido.builder()
                        .reporte(reporte)
                        .tipoContenido(TipoContenidoModerado.SERVICIO)
                        .servicio(servicio)
                        .moderador(moderador)
                        .motivo(motivoAccion)
                        .accion(AccionModeracionContenido.REACTIVADO)
                        .build());

                notificarDuenoContenido(
                        servicio.getUsuario(),
                        "CONTENIDO_REACTIVADO",
                        "Tu servicio fue reactivado",
                        agregarRespuestaModerador(
                                obtenerReferenciaServicioNotificacion(servicio) + " fue reactivado.",
                                mensajeRespuesta
                        ),
                        "SERVICIO",
                        servicio.getIdServicio()
                );
            }
            case USUARIO -> throw new BusinessException("La accion REACTIVAR_CONTENIDO no aplica a reportes de usuario");
        }
    }

    private void eliminarContenidoLogicamente(Reporte reporte, Usuario moderador, String motivoAccion, String mensajeRespuesta) {
        LocalDateTime ahora = LocalDateTime.now();

        switch (reporte.getTipoObjetivo()) {
            case OBRA -> {
                Obra obra = obtenerObraObjetivo(reporte);
                obra.setOculta(Boolean.TRUE);
                obra.setEstadoModeracion(EstadoModeracion.ELIMINADO_POR_MODERACION);
                obra.setMotivoOculta(motivoAccion);
                obra.setFechaOculta(ahora);
                obraRepository.save(obra);

                moderacionContenidoRepository.save(ModeracionContenido.builder()
                        .reporte(reporte)
                        .tipoContenido(TipoContenidoModerado.OBRA)
                        .obra(obra)
                        .moderador(moderador)
                        .motivo(motivoAccion)
                        .accion(AccionModeracionContenido.ELIMINADO_LOGICO)
                        .build());

                notificarDuenoContenido(
                        obra.getUsuario(),
                        "CONTENIDO_ELIMINADO_LOGICO",
                        "Tu obra fue retirada",
                        agregarRespuestaModerador(
                                obtenerReferenciaObraNotificacion(obra) + " fue retirada.",
                                mensajeRespuesta
                        ),
                        "OBRA",
                        obra.getIdObra()
                );
            }
            case SERVICIO -> {
                Servicio servicio = obtenerServicioObjetivo(reporte);
                servicio.setOculto(Boolean.TRUE);
                servicio.setEstadoModeracion(EstadoModeracion.ELIMINADO_POR_MODERACION);
                servicio.setMotivoOculto(motivoAccion);
                servicio.setFechaOculto(ahora);
                servicioRepository.save(servicio);

                moderacionContenidoRepository.save(ModeracionContenido.builder()
                        .reporte(reporte)
                        .tipoContenido(TipoContenidoModerado.SERVICIO)
                        .servicio(servicio)
                        .moderador(moderador)
                        .motivo(motivoAccion)
                        .accion(AccionModeracionContenido.ELIMINADO_LOGICO)
                        .build());

                notificarDuenoContenido(
                        servicio.getUsuario(),
                        "CONTENIDO_ELIMINADO_LOGICO",
                        "Tu servicio fue retirado",
                        agregarRespuestaModerador(
                                obtenerReferenciaServicioNotificacion(servicio) + " fue retirado.",
                                mensajeRespuesta
                        ),
                        "SERVICIO",
                        servicio.getIdServicio()
                );
            }
            case USUARIO -> throw new BusinessException("La accion ELIMINAR_CONTENIDO_LOGICO no aplica a reportes de usuario");
        }
    }

    private void advertirUsuarioAfectado(Reporte reporte, Usuario moderador, String motivoAccion, String mensajeRespuesta) {
        Usuario usuarioAfectado = obtenerUsuarioAfectadoParaSancion(reporte);

        suspensionUsuarioRepository.save(SuspensionUsuario.builder()
                .usuario(usuarioAfectado)
                .moderador(moderador)
                .reporte(reporte)
                .motivo(motivoAccion)
                .tipoSancion(TipoSancionUsuario.ADVERTENCIA)
                .estado(EstadoSuspensionUsuario.FINALIZADA)
                .fechaInicio(LocalDateTime.now())
                .fechaFin(null)
                .build());

        notificarUsuarioAfectado(
                usuarioAfectado,
                "USUARIO_ADVERTIDO",
                "Has recibido una advertencia",
                agregarRespuestaModerador("Tu cuenta recibio una advertencia.", mensajeRespuesta),
                "USUARIO",
                usuarioAfectado.getIdUsuario()
        );
    }

    private void suspenderUsuarioAfectado(Reporte reporte,
                                          Usuario moderador,
                                          String motivoAccion,
                                          LocalDateTime fechaFinSuspension,
                                          String mensajeRespuesta) {
        if (fechaFinSuspension == null) {
            throw new ResponseStatusException(BAD_REQUEST, "fechaFinSuspension es obligatoria para suspender usuario");
        }
        if (!fechaFinSuspension.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "fechaFinSuspension debe ser una fecha futura");
        }

        Usuario usuarioAfectado = obtenerUsuarioAfectadoParaSancion(reporte);
        if (usuarioAfectado.getEstadoCuenta() == EstadoCuenta.BLOQUEADO_PERMANENTE) {
            throw new BusinessException("No se puede suspender un usuario bloqueado permanentemente");
        }

        LocalDateTime ahora = LocalDateTime.now();
        usuarioAfectado.setEstadoCuenta(EstadoCuenta.SUSPENDIDO);
        usuarioAfectado.setMotivoSuspension(motivoAccion);
        usuarioAfectado.setFechaSuspension(ahora);
        usuarioAfectado.setFechaFinSuspension(fechaFinSuspension);
        usuarioRepository.save(usuarioAfectado);

        suspensionUsuarioRepository.save(SuspensionUsuario.builder()
                .usuario(usuarioAfectado)
                .moderador(moderador)
                .reporte(reporte)
                .motivo(motivoAccion)
                .tipoSancion(TipoSancionUsuario.SUSPENSION_TEMPORAL)
                .estado(EstadoSuspensionUsuario.ACTIVA)
                .fechaInicio(ahora)
                .fechaFin(fechaFinSuspension)
                .build());

        notificarUsuarioAfectado(
                usuarioAfectado,
                "USUARIO_SUSPENDIDO",
                "Tu cuenta fue suspendida",
                agregarRespuestaModerador("Tu cuenta fue suspendida temporalmente.", mensajeRespuesta),
                "USUARIO",
                usuarioAfectado.getIdUsuario()
        );
    }

    private void reactivarUsuarioAfectado(Reporte reporte, Usuario moderador, String motivoAccion, String mensajeRespuesta) {
        Usuario usuarioAfectado = obtenerUsuarioAfectadoParaSancion(reporte);
        if (usuarioAfectado.getEstadoCuenta() == EstadoCuenta.BLOQUEADO_PERMANENTE) {
            throw new BusinessException("No se puede reactivar un usuario bloqueado permanentemente");
        }
        if (usuarioAfectado.getEstadoCuenta() == EstadoCuenta.ACTIVO) {
            throw new BusinessException("El usuario ya se encuentra activo");
        }

        usuarioAfectado.setEstadoCuenta(EstadoCuenta.ACTIVO);
        usuarioAfectado.setMotivoSuspension(null);
        usuarioAfectado.setFechaSuspension(null);
        usuarioAfectado.setFechaFinSuspension(null);
        usuarioRepository.save(usuarioAfectado);

        suspensionUsuarioRepository.save(SuspensionUsuario.builder()
                .usuario(usuarioAfectado)
                .moderador(moderador)
                .reporte(reporte)
                .motivo(motivoAccion)
                .tipoSancion(TipoSancionUsuario.REACTIVACION)
                .estado(EstadoSuspensionUsuario.FINALIZADA)
                .fechaInicio(LocalDateTime.now())
                .fechaFin(null)
                .build());

        notificarUsuarioAfectado(
                usuarioAfectado,
                "USUARIO_REACTIVADO",
                "Tu cuenta fue reactivada",
                agregarRespuestaModerador("Tu cuenta fue reactivada.", mensajeRespuesta),
                "USUARIO",
                usuarioAfectado.getIdUsuario()
        );
    }

    private void bloquearUsuarioAfectado(Reporte reporte, Usuario moderador, String motivoAccion, String mensajeRespuesta) {
        Usuario usuarioAfectado = obtenerUsuarioAfectadoParaSancion(reporte);
        if (usuarioAfectado.getEstadoCuenta() == EstadoCuenta.BLOQUEADO_PERMANENTE) {
            throw new BusinessException("El usuario ya se encuentra bloqueado permanentemente");
        }

        LocalDateTime ahora = LocalDateTime.now();
        usuarioAfectado.setEstadoCuenta(EstadoCuenta.BLOQUEADO_PERMANENTE);
        usuarioAfectado.setMotivoSuspension(motivoAccion);
        usuarioAfectado.setFechaSuspension(ahora);
        usuarioAfectado.setFechaFinSuspension(null);
        usuarioRepository.save(usuarioAfectado);

        suspensionUsuarioRepository.save(SuspensionUsuario.builder()
                .usuario(usuarioAfectado)
                .moderador(moderador)
                .reporte(reporte)
                .motivo(motivoAccion)
                .tipoSancion(TipoSancionUsuario.BLOQUEO_PERMANENTE)
                .estado(EstadoSuspensionUsuario.ACTIVA)
                .fechaInicio(ahora)
                .fechaFin(null)
                .build());

        notificarUsuarioAfectado(
                usuarioAfectado,
                "USUARIO_BLOQUEADO",
                "Tu cuenta fue bloqueada",
                agregarRespuestaModerador("Tu cuenta fue bloqueada permanentemente.", mensajeRespuesta),
                "USUARIO",
                usuarioAfectado.getIdUsuario()
        );
    }

    private Obra obtenerObraObjetivo(Reporte reporte) {
        if (reporte.getTipoObjetivo() != TipoObjetivoReporte.OBRA || reporte.getObra() == null) {
            throw new ResourceNotFoundException("La obra asociada al reporte no fue encontrada");
        }
        return reporte.getObra();
    }

    private Servicio obtenerServicioObjetivo(Reporte reporte) {
        if (reporte.getTipoObjetivo() != TipoObjetivoReporte.SERVICIO || reporte.getServicio() == null) {
            throw new ResourceNotFoundException("El servicio asociado al reporte no fue encontrado");
        }
        return reporte.getServicio();
    }

    private Usuario obtenerUsuarioAfectadoParaSancion(Reporte reporte) {
        Usuario usuarioAfectado = obtenerUsuarioAfectado(reporte);
        if (usuarioAfectado == null || usuarioAfectado.getIdUsuario() == null) {
            throw new ResourceNotFoundException("El usuario afectado por el reporte no fue encontrado");
        }
        return usuarioAfectado;
    }

    private void notificarReportanteSegunResolucion(Reporte reporte,
                                                    AccionResolucionReporte accion,
                                                    String mensajeRespuesta) {
        if (reporte.getUsuarioReportante() == null || reporte.getUsuarioReportante().getIdUsuario() == null) {
            return;
        }

        if (accion == AccionResolucionReporte.DESCARTAR_REPORTE) {
            notificacionService.crearNotificacionSistema(
                    reporte.getUsuarioReportante().getIdUsuario(),
                    "REPORTE_DESCARTADO",
                    "Tu reporte fue descartado",
                    agregarRespuestaModerador(
                            "Tu reporte fue revisado y descartado.",
                            mensajeRespuesta
                    ),
                    "REPORTE",
                    reporte.getIdReporte()
            );
            return;
        }

        notificacionService.crearNotificacionSistema(
                reporte.getUsuarioReportante().getIdUsuario(),
                "REPORTE_RESUELTO",
                "Tu reporte fue resuelto",
                agregarRespuestaModerador(
                        "Tu reporte fue revisado y resuelto.",
                        mensajeRespuesta
                ),
                "REPORTE",
                reporte.getIdReporte()
        );
    }

    private void notificarDuenoContenido(Usuario destino,
                                         String tipoNotificacion,
                                         String titulo,
                                         String mensaje,
                                         String referenciaTipo,
                                         Integer referenciaId) {
        if (destino == null || destino.getIdUsuario() == null) {
            return;
        }

        notificacionService.crearNotificacionSistema(
                destino.getIdUsuario(),
                tipoNotificacion,
                titulo,
                mensaje,
                referenciaTipo,
                referenciaId
        );
    }

    private void notificarUsuarioAfectado(Usuario destino,
                                          String tipoNotificacion,
                                          String titulo,
                                          String mensaje,
                                          String referenciaTipo,
                                          Integer referenciaId) {
        if (destino == null || destino.getIdUsuario() == null) {
            return;
        }

        notificacionService.crearNotificacionSistema(
                destino.getIdUsuario(),
                tipoNotificacion,
                titulo,
                mensaje,
                referenciaTipo,
                referenciaId
        );
    }

    private String obtenerMensajeResolucion(AccionResolucionReporte accion) {
        return switch (accion) {
            case DESCARTAR_REPORTE -> "Reporte descartado correctamente";
            case OCULTAR_CONTENIDO -> "Reporte resuelto con ocultamiento de contenido";
            case REACTIVAR_CONTENIDO -> "Reporte resuelto con reactivacion de contenido";
            case ELIMINAR_CONTENIDO_LOGICO -> "Reporte resuelto con retiro de contenido";
            case ADVERTENCIA -> "Reporte resuelto con advertencia al usuario";
            case SUSPENDER_USUARIO -> "Reporte resuelto con suspension temporal del usuario";
            case REACTIVAR_USUARIO -> "Reporte resuelto con reactivacion del usuario";
            case BLOQUEAR_USUARIO_PERMANENTE -> "Reporte resuelto con bloqueo permanente del usuario";
            case SIN_ACCION -> "Accion invalida";
        };
    }

    private String obtenerReferenciaObraNotificacion(Obra obra) {
        String titulo = obra != null ? normalizarTextoOpcional(obra.getTitulo()) : null;
        if (titulo != null) {
            return "Tu obra \"" + titulo + "\"";
        }
        return "Esta obra";
    }

    private String obtenerReferenciaServicioNotificacion(Servicio servicio) {
        String titulo = servicio != null ? normalizarTextoOpcional(servicio.getTitulo()) : null;
        if (titulo != null) {
            return "Tu servicio \"" + titulo + "\"";
        }
        return "Este servicio";
    }

    private String agregarRespuestaModerador(String mensajeBase, String mensajeRespuesta) {
        return mensajeBase + " Respuesta: " + mensajeRespuesta;
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
