package com.example.demo.service.impl;

import com.example.demo.dto.meta.*;
import com.example.demo.enums.EstadoMetaPersonal;
import com.example.demo.enums.EstadoModeracion;
import com.example.demo.enums.TipoMetaPersonal;
import com.example.demo.model.MetaPersonalArtista;
import com.example.demo.model.Usuario;
import com.example.demo.repository.*;
import com.example.demo.service.MetaPersonalService;
import com.example.demo.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class MetaPersonalServiceImpl implements MetaPersonalService {

    private static final EnumSet<EstadoMetaPersonal> ESTADOS_ACTIVOS =
            EnumSet.of(EstadoMetaPersonal.POR_COMENZAR, EstadoMetaPersonal.EN_PROCESO);
    private static final EnumSet<EstadoMetaPersonal> ESTADOS_EDITABLES =
            EnumSet.of(EstadoMetaPersonal.POR_COMENZAR, EstadoMetaPersonal.EN_PROCESO);
    private static final EnumSet<EstadoMetaPersonal> ESTADOS_TERMINALES =
            EnumSet.of(EstadoMetaPersonal.COMPLETADA, EstadoMetaPersonal.EXPIRADA, EstadoMetaPersonal.CANCELADA);
    private static final List<String> ESTADOS_VENTA_COMPLETADA = List.of("CAPTURADA", "PAGADA", "COMPLETADA");
    private static final List<EstadoModeracion> ESTADOS_MODERACION_NO_VISIBLES =
            List.of(EstadoModeracion.OCULTO, EstadoModeracion.ELIMINADO_POR_MODERACION);
    private static final String REFERENCIA_META = "META_PERSONAL";
    private static final String TIPO_NOTIFICACION_COMPLETADA = "META_COMPLETADA";
    private static final String TIPO_NOTIFICACION_EXPIRADA = "META_EXPIRADA";

    private final MetaPersonalArtistaRepository metaPersonalArtistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CompraObraRepository compraObraRepository;
    private final CompraCarritoDetalleRepository compraCarritoDetalleRepository;
    private final ObraRepository obraRepository;
    private final ServicioRepository servicioRepository;
    private final FavoritosRepository favoritosRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;

    @Override
    public List<MetaPersonalDTO> listarMisMetas() {
        Usuario usuario = obtenerUsuarioAutenticado();
        return metaPersonalArtistaRepository.findByUsuarioIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario())
                .stream()
                .map(meta -> construirMetaDTO(meta, true, true))
                .toList();
    }

    @Override
    public MetaPersonalResumenDTO obtenerResumenMisMetas() {
        Usuario usuario = obtenerUsuarioAutenticado();
        List<MetaEvaluacion> evaluaciones = metaPersonalArtistaRepository
                .findByUsuarioIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario())
                .stream()
                .map(meta -> evaluarMeta(meta, true, true))
                .toList();

        int total = evaluaciones.size();
        int porComenzar = contarPorEstado(evaluaciones, EstadoMetaPersonal.POR_COMENZAR);
        int enProceso = contarPorEstado(evaluaciones, EstadoMetaPersonal.EN_PROCESO);
        int completadas = contarPorEstado(evaluaciones, EstadoMetaPersonal.COMPLETADA);
        int expiradas = contarPorEstado(evaluaciones, EstadoMetaPersonal.EXPIRADA);
        int canceladas = contarPorEstado(evaluaciones, EstadoMetaPersonal.CANCELADA);

        BigDecimal porcentajeGlobal = total == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : evaluaciones.stream()
                        .map(MetaEvaluacion::porcentajeVisual)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return MetaPersonalResumenDTO.builder()
                .total(total)
                .activas(porComenzar + enProceso)
                .porComenzar(porComenzar)
                .enProceso(enProceso)
                .completadas(completadas)
                .expiradas(expiradas)
                .canceladas(canceladas)
                .porcentajeGlobal(porcentajeGlobal)
                .build();
    }

    @Override
    public MetaPersonalDTO crearMeta(MetaPersonalRequestDTO request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        validarRequest(request.getTipoMeta(), request.getObjetivo(), request.getFechaInicio(), request.getFechaFin());
        validarMetaActivaDuplicada(usuario.getIdUsuario(), request.getTipoMeta(), null);

        MetaPersonalArtista meta = MetaPersonalArtista.builder()
                .usuario(usuario)
                .tipoMeta(request.getTipoMeta())
                .objetivo(normalizarNumero(request.getObjetivo()))
                .estado(calcularEstadoBase(request.getFechaInicio()))
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .build();

        MetaPersonalArtista guardada = metaPersonalArtistaRepository.saveAndFlush(meta);
        return construirMetaDTO(guardada, true, true);
    }

    @Override
    public MetaPersonalDTO actualizarMeta(Integer idMeta, MetaPersonalUpdateDTO request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        MetaPersonalArtista meta = obtenerMetaPropia(idMeta, usuario.getIdUsuario());

        if (!ESTADOS_EDITABLES.contains(meta.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden editar metas en estado POR_COMENZAR o EN_PROCESO."
            );
        }

        validarRequest(request.getTipoMeta(), request.getObjetivo(), request.getFechaInicio(), request.getFechaFin());
        validarMetaActivaDuplicada(usuario.getIdUsuario(), request.getTipoMeta(), meta.getIdMeta());

        meta.setTipoMeta(request.getTipoMeta());
        meta.setObjetivo(normalizarNumero(request.getObjetivo()));
        meta.setFechaInicio(request.getFechaInicio());
        meta.setFechaFin(request.getFechaFin());

        MetaPersonalArtista guardada = metaPersonalArtistaRepository.saveAndFlush(meta);
        return construirMetaDTO(guardada, true, true);
    }

    @Override
    public MetaPersonalDTO cancelarMeta(Integer idMeta, MetaPersonalCancelRequestDTO request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        MetaPersonalArtista meta = obtenerMetaPropia(idMeta, usuario.getIdUsuario());

        if (meta.getEstado() == EstadoMetaPersonal.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La meta ya fue cancelada.");
        }

        if (meta.getEstado() == EstadoMetaPersonal.COMPLETADA || meta.getEstado() == EstadoMetaPersonal.EXPIRADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede cancelar una meta completada o expirada."
            );
        }

        meta.setEstado(EstadoMetaPersonal.CANCELADA);
        meta.setFechaCancelacion(LocalDateTime.now());
        meta.setMotivoCancelacion(limpiarTexto(request != null ? request.getMotivoCancelacion() : null));

        MetaPersonalArtista guardada = metaPersonalArtistaRepository.saveAndFlush(meta);
        return construirMetaDTO(guardada, false, false);
    }

    @Override
    public MetaPersonalProgresoDTO obtenerProgresoMeta(Integer idMeta) {
        Usuario usuario = obtenerUsuarioAutenticado();
        MetaPersonalArtista meta = obtenerMetaPropia(idMeta, usuario.getIdUsuario());
        MetaEvaluacion evaluacion = evaluarMeta(meta, true, true);
        return aProgresoDTO(evaluacion);
    }

    @Override
    public void evaluarMetasDelUsuario(Integer idUsuario) {
        evaluarMetasDelUsuarioPorTipos(idUsuario, EnumSet.allOf(TipoMetaPersonal.class));
    }

    @Override
    public void evaluarMetasDelUsuarioPorTipos(Integer idUsuario, Set<TipoMetaPersonal> tipos) {
        if (idUsuario == null) {
            return;
        }

        EnumSet<TipoMetaPersonal> tiposObjetivo = (tipos == null || tipos.isEmpty())
                ? EnumSet.allOf(TipoMetaPersonal.class)
                : EnumSet.copyOf(tipos);

        List<MetaPersonalArtista> metasActivas = metaPersonalArtistaRepository.findActivasByUsuarioYTiposForUpdate(
                idUsuario,
                ESTADOS_ACTIVOS,
                tiposObjetivo
        );

        for (MetaPersonalArtista meta : metasActivas) {
            evaluarMeta(meta, true, true);
        }
    }

    private MetaPersonalDTO construirMetaDTO(MetaPersonalArtista meta, boolean actualizarEstado, boolean permitirNotificacion) {
        MetaEvaluacion evaluacion = evaluarMeta(meta, actualizarEstado, permitirNotificacion);
        return MetaPersonalDTO.builder()
                .idMeta(evaluacion.meta().getIdMeta())
                .tipoMeta(evaluacion.meta().getTipoMeta())
                .objetivo(evaluacion.meta().getObjetivo())
                .estado(evaluacion.meta().getEstado())
                .fechaInicio(evaluacion.meta().getFechaInicio())
                .fechaFin(evaluacion.meta().getFechaFin())
                .fechaCreacion(evaluacion.meta().getFechaCreacion())
                .fechaActualizacion(evaluacion.meta().getFechaActualizacion())
                .fechaCancelacion(evaluacion.meta().getFechaCancelacion())
                .motivoCancelacion(evaluacion.meta().getMotivoCancelacion())
                .progresoActual(evaluacion.progresoActual())
                .porcentaje(evaluacion.porcentajeReal())
                .porcentajeVisual(evaluacion.porcentajeVisual())
                .progresoTexto(evaluacion.progresoTexto())
                .mensajeEstado(evaluacion.mensajeEstado())
                .editable(esEditable(evaluacion.meta().getEstado()))
                .cancelable(esEditable(evaluacion.meta().getEstado()))
                .build();
    }

    private MetaPersonalProgresoDTO aProgresoDTO(MetaEvaluacion evaluacion) {
        return MetaPersonalProgresoDTO.builder()
                .idMeta(evaluacion.meta().getIdMeta())
                .tipoMeta(evaluacion.meta().getTipoMeta())
                .objetivo(evaluacion.meta().getObjetivo())
                .estado(evaluacion.meta().getEstado())
                .fechaInicio(evaluacion.meta().getFechaInicio())
                .fechaFin(evaluacion.meta().getFechaFin())
                .progresoActual(evaluacion.progresoActual())
                .porcentaje(evaluacion.porcentajeReal())
                .porcentajeVisual(evaluacion.porcentajeVisual())
                .progresoTexto(evaluacion.progresoTexto())
                .mensajeEstado(evaluacion.mensajeEstado())
                .editable(esEditable(evaluacion.meta().getEstado()))
                .cancelable(esEditable(evaluacion.meta().getEstado()))
                .build();
    }

    private MetaEvaluacion evaluarMeta(MetaPersonalArtista meta, boolean actualizarEstado, boolean permitirNotificacion) {
        BigDecimal progresoActual = calcularProgreso(meta);
        BigDecimal porcentajeReal = calcularPorcentajeReal(progresoActual, meta.getObjetivo());
        BigDecimal porcentajeVisual = limitarPorcentajeVisual(porcentajeReal);

        MetaPersonalArtista metaEvaluada = meta;
        EstadoMetaPersonal estadoAnterior = meta.getEstado();

        if (actualizarEstado && !ESTADOS_TERMINALES.contains(estadoAnterior)) {
            EstadoMetaPersonal estadoCalculado = resolverEstado(meta, progresoActual);
            if (estadoCalculado != estadoAnterior) {
                meta.setEstado(estadoCalculado);
                metaEvaluada = metaPersonalArtistaRepository.saveAndFlush(meta);
                manejarNotificacionPorTransicion(metaEvaluada, estadoAnterior, estadoCalculado, progresoActual, porcentajeVisual, permitirNotificacion);
            }
        }

        return new MetaEvaluacion(
                metaEvaluada,
                progresoActual,
                porcentajeReal,
                porcentajeVisual,
                construirProgresoTexto(metaEvaluada.getTipoMeta(), progresoActual, metaEvaluada.getObjetivo()),
                construirMensajeEstado(metaEvaluada.getEstado())
        );
    }

    private EstadoMetaPersonal resolverEstado(MetaPersonalArtista meta, BigDecimal progresoActual) {
        LocalDate hoy = LocalDate.now();
        if (progresoActual.compareTo(meta.getObjetivo()) >= 0) {
            return EstadoMetaPersonal.COMPLETADA;
        }
        if (hoy.isAfter(meta.getFechaFin())) {
            return EstadoMetaPersonal.EXPIRADA;
        }
        if (hoy.isBefore(meta.getFechaInicio())) {
            return EstadoMetaPersonal.POR_COMENZAR;
        }
        return EstadoMetaPersonal.EN_PROCESO;
    }

    private BigDecimal calcularProgreso(MetaPersonalArtista meta) {
        LocalDateTime inicio = meta.getFechaInicio().atStartOfDay();
        LocalDateTime fin = meta.getFechaFin().atTime(LocalTime.MAX);
        Integer idUsuario = meta.getUsuario().getIdUsuario();

        return switch (meta.getTipoMeta()) {
            case VENTAS -> BigDecimal.valueOf(
                    compraObraRepository.countVentasCompletadasByVendedorYPeriodo(idUsuario, ESTADOS_VENTA_COMPLETADA, inicio, fin)
                            + compraCarritoDetalleRepository.countVentasCompletadasByVendedorYPeriodo(idUsuario, ESTADOS_VENTA_COMPLETADA, inicio, fin)
            );
            case INGRESOS -> valorSeguro(
                    compraObraRepository.sumIngresosCompletadosByVendedorYPeriodo(idUsuario, ESTADOS_VENTA_COMPLETADA, inicio, fin))
                    .add(valorSeguro(
                            compraCarritoDetalleRepository.sumIngresosCompletadosByVendedorYPeriodo(idUsuario, ESTADOS_VENTA_COMPLETADA, inicio, fin)
                    ));
            case PUBLICACIONES -> BigDecimal.valueOf(
                    obraRepository.countPublicadasByUsuarioYPeriodo(idUsuario, inicio, fin)
                            + servicioRepository.countPublicadosByUsuarioYPeriodo(idUsuario, inicio, fin)
            );
            case FAVORITOS -> BigDecimal.valueOf(
                    favoritosRepository.countFavoritosDirectosRecibidosEnPeriodo(idUsuario, inicio, fin)
                            + favoritosRepository.countFavoritosObrasVisiblesRecibidosEnPeriodo(
                            idUsuario, inicio, fin, ESTADOS_MODERACION_NO_VISIBLES)
                            + favoritosRepository.countFavoritosServiciosVisiblesRecibidosEnPeriodo(
                            idUsuario, inicio, fin, ESTADOS_MODERACION_NO_VISIBLES)
            );
        };
    }

    private void enviarNotificacionSiNoExiste(
            MetaPersonalArtista meta,
            EstadoMetaPersonal estadoFinal,
            BigDecimal progresoActual,
            BigDecimal porcentajeVisual
    ) {
        String tipoNotificacion = estadoFinal == EstadoMetaPersonal.COMPLETADA
                ? TIPO_NOTIFICACION_COMPLETADA
                : TIPO_NOTIFICACION_EXPIRADA;

        boolean yaExiste = notificacionRepository
                .existsByUsuarioDestinoIdUsuarioAndReferenciaTipoIgnoreCaseAndReferenciaIdAndTipoNotificacionIgnoreCase(
                        meta.getUsuario().getIdUsuario(),
                        REFERENCIA_META,
                        meta.getIdMeta(),
                        tipoNotificacion
                );

        if (yaExiste) {
            return;
        }

        String titulo = estadoFinal == EstadoMetaPersonal.COMPLETADA
                ? "\u00a1Meta completada!"
                : "Meta expirada";

        String detalle = construirDetalleNotificacion(meta.getTipoMeta(), progresoActual, meta.getObjetivo());
        String porcentajeTexto = formatearNumero(porcentajeVisual);

        String mensaje = estadoFinal == EstadoMetaPersonal.COMPLETADA
                ? "\u00a1Felicidades! Alcanzaste tu meta de " + obtenerTipoMetaEnMinusculas(meta.getTipoMeta())
                + " con un avance del " + porcentajeTexto + "%. Lograste " + detalle + "."
                : "Tu meta de " + obtenerTipoMetaEnMinusculas(meta.getTipoMeta())
                + " finaliz\u00f3 con un avance del " + porcentajeTexto + "%. Lograste " + detalle
                + ". Puedes crear una nueva meta y seguir avanzando.";

        notificacionService.crearNotificacionSistema(
                meta.getUsuario().getIdUsuario(),
                tipoNotificacion,
                titulo,
                mensaje,
                REFERENCIA_META,
                meta.getIdMeta()
        );
    }

    private void manejarNotificacionPorTransicion(
            MetaPersonalArtista meta,
            EstadoMetaPersonal estadoAnterior,
            EstadoMetaPersonal estadoNuevo,
            BigDecimal progresoActual,
            BigDecimal porcentajeVisual,
            boolean permitirNotificacion
    ) {
        if (!permitirNotificacion) {
            return;
        }

        if (estadoAnterior == EstadoMetaPersonal.CANCELADA
                || estadoAnterior == EstadoMetaPersonal.COMPLETADA
                || estadoAnterior == EstadoMetaPersonal.EXPIRADA) {
            return;
        }

        if (estadoNuevo == EstadoMetaPersonal.COMPLETADA || estadoNuevo == EstadoMetaPersonal.EXPIRADA) {
            enviarNotificacionSiNoExiste(meta, estadoNuevo, progresoActual, porcentajeVisual);
        }
    }

    private String construirDetalleNotificacion(TipoMetaPersonal tipoMeta, BigDecimal progresoActual, BigDecimal objetivo) {
        if (tipoMeta == TipoMetaPersonal.INGRESOS) {
            return formatearMoneda(progresoActual) + " de " + formatearMoneda(objetivo);
        }
        return formatearNumero(progresoActual) + " de " + formatearNumero(objetivo) + " " + obtenerTipoMetaEnMinusculas(tipoMeta);
    }

    private String construirProgresoTexto(TipoMetaPersonal tipoMeta, BigDecimal progresoActual, BigDecimal objetivo) {
        if (tipoMeta == TipoMetaPersonal.INGRESOS) {
            return formatearMoneda(progresoActual) + " / " + formatearMoneda(objetivo);
        }
        return formatearNumero(progresoActual) + " / " + formatearNumero(objetivo) + " " + obtenerTipoMetaEnMinusculas(tipoMeta);
    }

    private String construirMensajeEstado(EstadoMetaPersonal estado) {
        return switch (estado) {
            case POR_COMENZAR -> "La meta aun no inicia.";
            case EN_PROCESO -> "La meta esta en progreso.";
            case COMPLETADA -> "La meta ya fue completada.";
            case EXPIRADA -> "La meta finalizo sin alcanzar el objetivo.";
            case CANCELADA -> "La meta fue cancelada por el usuario.";
        };
    }

    private BigDecimal calcularPorcentajeReal(BigDecimal progresoActual, BigDecimal objetivo) {
        if (objetivo == null || objetivo.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return progresoActual
                .multiply(BigDecimal.valueOf(100))
                .divide(objetivo, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal limitarPorcentajeVisual(BigDecimal porcentajeReal) {
        BigDecimal porcentaje = porcentajeReal.max(BigDecimal.ZERO);
        return porcentaje.compareTo(BigDecimal.valueOf(100)) > 0
                ? BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP)
                : porcentaje.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean esEditable(EstadoMetaPersonal estado) {
        return ESTADOS_EDITABLES.contains(estado);
    }

    private void validarRequest(TipoMetaPersonal tipoMeta, BigDecimal objetivo, LocalDate fechaInicio, LocalDate fechaFin) {
        if (tipoMeta == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de meta es obligatorio.");
        }
        if (objetivo == null || objetivo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El objetivo debe ser mayor a 0.");
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las fechas de inicio y fin son obligatorias.");
        }
        if (fechaFin.isBefore(fechaInicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha fin no puede ser menor que la fecha inicio.");
        }
    }

    private void validarMetaActivaDuplicada(Integer idUsuario, TipoMetaPersonal tipoMeta, Integer idMetaActual) {
        boolean existe = idMetaActual == null
                ? metaPersonalArtistaRepository.existsByUsuarioIdUsuarioAndTipoMetaAndEstadoIn(idUsuario, tipoMeta, ESTADOS_ACTIVOS)
                : metaPersonalArtistaRepository.existsByUsuarioIdUsuarioAndTipoMetaAndEstadoInAndIdMetaNot(
                idUsuario,
                tipoMeta,
                ESTADOS_ACTIVOS,
                idMetaActual
        );

        if (existe) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una meta activa del mismo tipo para el usuario autenticado."
            );
        }
    }

    private MetaPersonalArtista obtenerMetaPropia(Integer idMeta, Integer idUsuario) {
        MetaPersonalArtista meta = metaPersonalArtistaRepository.findById(idMeta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta no encontrada."));

        if (!Objects.equals(meta.getUsuario().getIdUsuario(), idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta meta.");
        }
        return meta;
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se encontro un usuario autenticado.");
        }

        Integer idUsuario;
        try {
            idUsuario = Integer.valueOf(String.valueOf(authentication.getPrincipal()));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al usuario autenticado.");
        }

        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado."));
    }

    private EstadoMetaPersonal calcularEstadoBase(LocalDate fechaInicio) {
        return LocalDate.now().isBefore(fechaInicio)
                ? EstadoMetaPersonal.POR_COMENZAR
                : EstadoMetaPersonal.EN_PROCESO;
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private BigDecimal normalizarNumero(BigDecimal valor) {
        return valor == null ? null : valor.stripTrailingZeros().scale() < 0 ? valor.setScale(0, RoundingMode.HALF_UP) : valor.stripTrailingZeros();
    }

    private String limpiarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String obtenerTipoMetaEnMinusculas(TipoMetaPersonal tipoMeta) {
        return switch (tipoMeta) {
            case VENTAS -> "ventas";
            case PUBLICACIONES -> "publicaciones";
            case INGRESOS -> "ingresos";
            case FAVORITOS -> "favoritos";
        };
    }

    private String formatearNumero(BigDecimal valor) {
        if (valor == null) {
            return "0";
        }
        BigDecimal normalizado = valor.stripTrailingZeros();
        return normalizado.scale() <= 0 ? normalizado.toPlainString() : normalizado.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatearMoneda(BigDecimal valor) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat decimalFormat = new DecimalFormat("$#,##0.##", symbols);
        return decimalFormat.format(valor == null ? BigDecimal.ZERO : valor);
    }

    private int contarPorEstado(List<MetaEvaluacion> evaluaciones, EstadoMetaPersonal estado) {
        return (int) evaluaciones.stream()
                .filter(evaluacion -> evaluacion.meta().getEstado() == estado)
                .count();
    }

    private record MetaEvaluacion(
            MetaPersonalArtista meta,
            BigDecimal progresoActual,
            BigDecimal porcentajeReal,
            BigDecimal porcentajeVisual,
            String progresoTexto,
            String mensajeEstado
    ) {
    }
}
