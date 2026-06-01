package com.example.demo.service.impl;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dto.admin.*;
import com.example.demo.enums.AdminDashboardTipo;
import com.example.demo.enums.AdminTipoDatoEstadistica;
import com.example.demo.enums.AdminTipoEstadistica;
import com.example.demo.model.AdminObservacionEstadistica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AdminEstadisticasRepository;
import com.example.demo.repository.AdminObservacionEstadisticaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.AdminEstadisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminEstadisticasServiceImpl implements AdminEstadisticasService {

    private static final int LIMIT_DEFAULT = 5;
    private static final int LIMIT_MAX = 50;
    private static final Locale LOCALE_ES = new Locale("es", "MX");

    private final AdminEstadisticasRepository adminEstadisticasRepository;
    private final AdminObservacionEstadisticaRepository adminObservacionEstadisticaRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminCategoriaStatsDTO> obtenerCantidadPorCategoria(AdminDashboardTipo tipo) {
        List<AdminCategoriaStatsDTO> resultados = switch (tipo) {
            case OBRAS -> mapearCategoriaStats(adminEstadisticasRepository.obtenerObrasPorCategoria());
            case SERVICIOS -> mapearCategoriaStats(adminEstadisticasRepository.obtenerServiciosPorCategoria());
            case ARTISTAS -> {
                List<AdminCategoriaStatsDTO> items = new ArrayList<>(mapearCategoriaStats(adminEstadisticasRepository.obtenerArtistasPorCategoria()));
                long sinCategoria = toLong(adminEstadisticasRepository.contarArtistasSinCategoria());
                if (sinCategoria > 0) {
                    items.add(AdminCategoriaStatsDTO.builder()
                            .idCategoria(null)
                            .categoria("Sin categoria")
                            .total(sinCategoria)
                            .build());
                }
                items.sort(Comparator.comparingLong(AdminCategoriaStatsDTO::getTotal).reversed()
                        .thenComparing(dto -> dto.getCategoria() == null ? "" : dto.getCategoria()));
                yield items;
            }
        };

        return resultados;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSerieTemporalDTO obtenerVentasSemanales(LocalDate fechaReferencia) {
        LocalDate fechaBase = fechaReferencia != null ? fechaReferencia : LocalDate.now();
        RangoSemana rangoSemana = validarYConstruirSemana(fechaBase);

        Map<LocalDate, ConteoMonto> acumulados = crearMapaSemanalVacio(rangoSemana.inicio(), rangoSemana.fin());
        sumarSerieVentas(acumulados, adminEstadisticasRepository.obtenerVentasDirectasPorDia(rangoSemana.inicioDateTime(), rangoSemana.finExclusiveDateTime()));
        sumarSerieVentas(acumulados, adminEstadisticasRepository.obtenerVentasCarritoPorDia(rangoSemana.inicioDateTime(), rangoSemana.finExclusiveDateTime()));

        long totalVentas = acumulados.values().stream().mapToLong(ConteoMonto::conteo).sum();
        BigDecimal totalIngresos = acumulados.values().stream()
                .map(ConteoMonto::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AdminPuntoSerieDTO> puntos = construirPuntosSerie(acumulados);
        String mensaje = totalVentas > 0
                ? "Se registraron " + totalVentas + " ventas completadas en la semana seleccionada."
                : "No se registraron ventas completadas en la semana seleccionada.";

        return AdminSerieTemporalDTO.builder()
                .fechaReferencia(fechaBase)
                .fechaInicioPeriodo(rangoSemana.inicio())
                .fechaFinPeriodo(rangoSemana.fin())
                .puntos(puntos)
                .totalVentas(totalVentas)
                .totalIngresos(totalIngresos)
                .mensaje(mensaje)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminRankingResponseDTO obtenerRanking(AdminDashboardTipo tipo, Integer limit) {
        int limiteAplicado = normalizarLimit(limit);
        List<AdminRankingItemDTO> items = switch (tipo) {
            case OBRAS -> mapearRanking(adminEstadisticasRepository.obtenerRankingObras(limiteAplicado));
            case SERVICIOS -> mapearRanking(adminEstadisticasRepository.obtenerRankingServicios(limiteAplicado));
            case ARTISTAS -> mapearRanking(adminEstadisticasRepository.obtenerRankingArtistas(limiteAplicado));
        };

        String mensaje = items.isEmpty()
                ? "No hay datos de popularidad disponibles para el criterio seleccionado."
                : "Se encontraron " + items.size() + " elementos en el ranking solicitado.";

        return AdminRankingResponseDTO.builder()
                .tipo(tipo)
                .limit(limiteAplicado)
                .items(items)
                .mensaje(mensaje)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCrecimientoDTO obtenerCrecimientoSemanal(AdminDashboardTipo tipo, LocalDate fechaReferencia) {
        LocalDate fechaBase = fechaReferencia != null ? fechaReferencia : LocalDate.now();
        RangoSemana semanaActual = validarYConstruirSemana(fechaBase);
        RangoSemana semanaAnterior = construirSemanaAnterior(semanaActual);

        Map<LocalDate, Long> serieActual = switch (tipo) {
            case OBRAS -> construirSerieConteo(semanaActual.inicio(), semanaActual.fin(),
                    adminEstadisticasRepository.obtenerPublicacionesObrasPorDia(
                            semanaActual.inicio(), semanaActual.fin()));
            case SERVICIOS -> construirSerieConteo(semanaActual.inicio(), semanaActual.fin(),
                    adminEstadisticasRepository.obtenerPublicacionesServiciosPorDia(
                            semanaActual.inicio(), semanaActual.fin()));
            case ARTISTAS -> construirSerieConteo(semanaActual.inicio(), semanaActual.fin(),
                    adminEstadisticasRepository.obtenerArtistasNuevosPorDia(
                            semanaActual.inicio(), semanaActual.fin()));
        };

        Map<LocalDate, Long> serieAnterior = switch (tipo) {
            case OBRAS -> construirSerieConteo(semanaAnterior.inicio(), semanaAnterior.fin(),
                    adminEstadisticasRepository.obtenerPublicacionesObrasPorDia(
                            semanaAnterior.inicio(), semanaAnterior.fin()));
            case SERVICIOS -> construirSerieConteo(semanaAnterior.inicio(), semanaAnterior.fin(),
                    adminEstadisticasRepository.obtenerPublicacionesServiciosPorDia(
                            semanaAnterior.inicio(), semanaAnterior.fin()));
            case ARTISTAS -> construirSerieConteo(semanaAnterior.inicio(), semanaAnterior.fin(),
                    adminEstadisticasRepository.obtenerArtistasNuevosPorDia(
                            semanaAnterior.inicio(), semanaAnterior.fin()));
        };

        long totalActual = serieActual.values().stream().mapToLong(Long::longValue).sum();
        long totalAnterior = serieAnterior.values().stream().mapToLong(Long::longValue).sum();

        Double porcentajeCambio = null;
        boolean periodoAnteriorSinDatos = totalAnterior == 0;
        String mensaje;
        String etiquetaComparacion = "respecto a la semana anterior.";

        if (totalAnterior == 0 && totalActual > 0) {
            mensaje = "No hay datos de la semana anterior, pero en la semana seleccionada si hubo actividad.";
        } else if (totalAnterior == 0) {
            porcentajeCambio = 0D;
            mensaje = "Sin actividad registrada en ambas semanas.";
        } else {
            porcentajeCambio = ((double) (totalActual - totalAnterior) / totalAnterior) * 100D;
            if (porcentajeCambio > 0) {
                mensaje = "La actividad crecio " + redondear(porcentajeCambio) + "% " + etiquetaComparacion;
            } else if (porcentajeCambio < 0) {
                mensaje = "La actividad disminuyo " + redondear(Math.abs(porcentajeCambio)) + "% " + etiquetaComparacion;
            } else {
                mensaje = "La actividad se mantuvo igual " + etiquetaComparacion;
            }
        }

        return AdminCrecimientoDTO.builder()
                .tipo(tipo)
                .fechaReferencia(fechaBase)
                .fechaInicioSemanaActual(semanaActual.inicio())
                .fechaFinSemanaActual(semanaActual.fin())
                .fechaInicioSemanaAnterior(semanaAnterior.inicio())
                .fechaFinSemanaAnterior(semanaAnterior.fin())
                .serieSemanaActual(construirPuntosConteo(serieActual))
                .serieSemanaAnterior(construirPuntosConteo(serieAnterior))
                .diasComparados(7)
                .totalSemanaActual(totalActual)
                .totalSemanaAnterior(totalAnterior)
                .porcentajeCambio(porcentajeCambio != null ? redondear(porcentajeCambio) : null)
                .periodoAnteriorSinDatos(periodoAnteriorSinDatos)
                .mensaje(mensaje)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminObservacionDTO> listarObservaciones(AdminTipoEstadistica tipoEstadistica,
                                                         AdminTipoDatoEstadistica tipoDato,
                                                         LocalDate fechaInicioPeriodo,
                                                         LocalDate fechaFinPeriodo) {
        return adminObservacionEstadisticaRepository.findAllByFiltros(
                        tipoEstadistica,
                        tipoDato,
                        fechaInicioPeriodo,
                        fechaFinPeriodo
                ).stream()
                .map(this::aObservacionDto)
                .toList();
    }

    @Override
    @Transactional
    public AdminObservacionDTO crearObservacion(AdminObservacionRequestDTO request) {
        validarRangoObservacion(request.getFechaInicioPeriodo(), request.getFechaFinPeriodo());
        Usuario admin = obtenerAdminAutenticado();

        adminObservacionEstadisticaRepository.findByContexto(
                        request.getTipoEstadistica(),
                        request.getTipoDato(),
                        request.getFechaInicioPeriodo(),
                        request.getFechaFinPeriodo()
                )
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe una observacion para el contexto seleccionado.");
                });

        AdminObservacionEstadistica observacion = AdminObservacionEstadistica.builder()
                .admin(admin)
                .tipoEstadistica(request.getTipoEstadistica())
                .tipoDato(request.getTipoDato())
                .fechaInicioPeriodo(request.getFechaInicioPeriodo())
                .fechaFinPeriodo(request.getFechaFinPeriodo())
                .observacion(request.getObservacion().trim())
                .build();

        return aObservacionDto(adminObservacionEstadisticaRepository.saveAndFlush(observacion));
    }

    @Override
    @Transactional
    public AdminObservacionDTO actualizarObservacion(Integer idObservacion, AdminObservacionRequestDTO request) {
        validarRangoObservacion(request.getFechaInicioPeriodo(), request.getFechaFinPeriodo());
        Usuario adminEditor = obtenerAdminAutenticado();

        AdminObservacionEstadistica observacion = adminObservacionEstadisticaRepository.findById(idObservacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Observacion no encontrada."));

        adminObservacionEstadisticaRepository.findByContexto(
                        request.getTipoEstadistica(),
                        request.getTipoDato(),
                        request.getFechaInicioPeriodo(),
                        request.getFechaFinPeriodo()
                )
                .filter(existing -> !existing.getIdObservacion().equals(idObservacion))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe otra observacion para el contexto seleccionado.");
                });

        observacion.setTipoEstadistica(request.getTipoEstadistica());
        observacion.setTipoDato(request.getTipoDato());
        observacion.setFechaInicioPeriodo(request.getFechaInicioPeriodo());
        observacion.setFechaFinPeriodo(request.getFechaFinPeriodo());
        observacion.setObservacion(request.getObservacion().trim());
        observacion.setAdmin(adminEditor);
        observacion.setFechaActualizacion(LocalDateTime.now());

        AdminObservacionEstadistica observacionGuardada = adminObservacionEstadisticaRepository.saveAndFlush(observacion);
        AdminObservacionEstadistica observacionActualizada = adminObservacionEstadisticaRepository.findDetalleById(observacionGuardada.getIdObservacion())
                .orElse(observacionGuardada);

        return aObservacionDto(observacionActualizada);
    }

    @Override
    @Transactional
    public void eliminarObservacion(Integer idObservacion) {
        obtenerAdminAutenticado();
        AdminObservacionEstadistica observacion = adminObservacionEstadisticaRepository.findById(idObservacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Observacion no encontrada."));
        adminObservacionEstadisticaRepository.delete(observacion);
    }

    private List<AdminCategoriaStatsDTO> mapearCategoriaStats(List<Object[]> filas) {
        return filas.stream()
                .map(fila -> AdminCategoriaStatsDTO.builder()
                        .idCategoria(toInteger(fila[0]))
                        .categoria(toStringValue(fila[1]))
                        .total(toLong(fila[2]))
                        .build())
                .toList();
    }

    private void sumarSerieVentas(Map<LocalDate, ConteoMonto> acumulados, List<Object[]> filas) {
        for (Object[] fila : filas) {
            LocalDate fecha = toLocalDate(fila[0]);
            ConteoMonto actual = acumulados.getOrDefault(fecha, new ConteoMonto(0, BigDecimal.ZERO));
            long conteo = actual.conteo() + toLong(fila[1]);
            BigDecimal monto = actual.monto().add(toBigDecimal(fila[2]));
            acumulados.put(fecha, new ConteoMonto(conteo, monto));
        }
    }

    private Map<LocalDate, ConteoMonto> crearMapaSemanalVacio(LocalDate inicio, LocalDate fin) {
        Map<LocalDate, ConteoMonto> mapa = new LinkedHashMap<>();
        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            mapa.put(fecha, new ConteoMonto(0, BigDecimal.ZERO));
        }
        return mapa;
    }

    private List<AdminPuntoSerieDTO> construirPuntosSerie(Map<LocalDate, ConteoMonto> serie) {
        return serie.entrySet().stream()
                .map(entry -> AdminPuntoSerieDTO.builder()
                        .fecha(entry.getKey())
                        .etiqueta(formatearEtiqueta(entry.getKey()))
                        .valor(entry.getValue().conteo())
                        .monto(entry.getValue().monto())
                        .build())
                .toList();
    }

    private List<AdminRankingItemDTO> mapearRanking(List<Object[]> filas) {
        return filas.stream()
                .map(fila -> AdminRankingItemDTO.builder()
                        .id(toInteger(fila[0]))
                        .nombre(toStringValue(fila[1]))
                        .total(toLong(fila[2]))
                        .descripcionSecundaria(toStringValue(fila[5]))
                        .imagen(toStringValue(fila[3]))
                        .imagenAutor(toStringValue(fila.length > 8 ? fila[8] : null))
                        .autor(toStringValue(fila[4]))
                        .subtitulo(toStringValue(fila[5]))
                        .contacto(toStringValue(fila.length > 6 ? fila[6] : null))
                        .tipoContacto(toStringValue(fila.length > 7 ? fila[7] : null))
                        .build())
                .toList();
    }

    private Map<LocalDate, Long> construirSerieConteo(LocalDate inicio, LocalDate fin, List<Object[]> filas) {
        Map<LocalDate, Long> serie = new LinkedHashMap<>();
        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            serie.put(fecha, 0L);
        }
        for (Object[] fila : filas) {
            serie.put(toLocalDate(fila[0]), toLong(fila[1]));
        }
        return serie;
    }

    private List<AdminPuntoSerieDTO> construirPuntosConteo(Map<LocalDate, Long> serie) {
        return serie.entrySet().stream()
                .map(entry -> AdminPuntoSerieDTO.builder()
                        .fecha(entry.getKey())
                        .etiqueta(formatearEtiqueta(entry.getKey()))
                        .valor(entry.getValue())
                        .monto(null)
                        .build())
                .toList();
    }

    private String formatearEtiqueta(LocalDate fecha) {
        return fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, LOCALE_ES);
    }

    private RangoSemana validarYConstruirSemana(LocalDate fechaReferencia) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = fechaReferencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate inicioSemanaActual = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        if (inicioSemana.isAfter(inicioSemanaActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permiten semanas futuras.");
        }

        LocalDate finSemana = inicioSemana.plusDays(6);
        return new RangoSemana(inicioSemana, finSemana);
    }

    private RangoSemana construirSemanaAnterior(RangoSemana rangoActual) {
        LocalDate inicioAnterior = rangoActual.inicio().minusWeeks(1);
        return new RangoSemana(inicioAnterior, inicioAnterior.plusDays(6));
    }

    private void validarRangoObservacion(LocalDate fechaInicioPeriodo, LocalDate fechaFinPeriodo) {
        if (fechaInicioPeriodo != null && fechaFinPeriodo != null && fechaFinPeriodo.isBefore(fechaInicioPeriodo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha_fin_periodo no puede ser menor que la fecha_inicio_periodo.");
        }
    }

    private Usuario obtenerAdminAutenticado() {
        Integer idUsuario = SecurityUtils.obtenerIdUsuarioAutenticado();
        Usuario admin = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));

        if (admin.getRol() == null || !"ADMIN".equalsIgnoreCase(admin.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un administrador puede gestionar observaciones.");
        }

        return admin;
    }

    private AdminObservacionDTO aObservacionDto(AdminObservacionEstadistica observacion) {
        Usuario admin = observacion.getAdmin();
        String nombreAdmin = null;
        if (admin != null) {
            nombreAdmin = admin.getNombreCompleto() != null && !admin.getNombreCompleto().isBlank()
                    ? admin.getNombreCompleto()
                    : admin.getUsuario();
        }

        return AdminObservacionDTO.builder()
                .idObservacion(observacion.getIdObservacion())
                .idAdmin(admin != null ? admin.getIdUsuario() : null)
                .nombreAdmin(nombreAdmin)
                .tipoEstadistica(observacion.getTipoEstadistica())
                .tipoDato(observacion.getTipoDato())
                .fechaInicioPeriodo(observacion.getFechaInicioPeriodo())
                .fechaFinPeriodo(observacion.getFechaFinPeriodo())
                .observacion(observacion.getObservacion())
                .fechaCreacion(observacion.getFechaCreacion())
                .fechaActualizacion(observacion.getFechaActualizacion())
                .build();
    }

    private int normalizarLimit(Integer limit) {
        int valor = limit == null ? LIMIT_DEFAULT : limit;
        if (valor <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El parametro limit debe ser mayor que 0.");
        }
        return Math.min(valor, LIMIT_MAX);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal
                ? decimal
                : BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private String toStringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Double redondear(Double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private record ConteoMonto(long conteo, BigDecimal monto) {
    }

    private record RangoSemana(LocalDate inicio, LocalDate fin) {
        LocalDateTime inicioDateTime() {
            return inicio.atStartOfDay();
        }

        LocalDateTime finExclusiveDateTime() {
            return fin.plusDays(1).atStartOfDay();
        }
    }

}
