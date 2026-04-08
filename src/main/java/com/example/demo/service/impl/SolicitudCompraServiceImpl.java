package com.example.demo.service.impl;

import com.example.demo.dto.solicitud.CrearSolicitudCompraRequestDTO;
import com.example.demo.dto.solicitud.SolicitudCompraDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Carrito;
import com.example.demo.model.Obra;
import com.example.demo.model.SolicitudCompraObra;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.SolicitudCompraObraRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.CarritoService;
import com.example.demo.service.NotificacionService;
import com.example.demo.service.SolicitudCompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitudCompraServiceImpl implements SolicitudCompraService {

    private static final int DIAS_RESERVA = 7;
    private static final int MAX_INTENTOS_FALLIDOS_POR_OBRA = 3;

    private final SolicitudCompraObraRepository solicitudRepository;
    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final CarritoService carritoService;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public SolicitudCompraDTO crearSolicitud(CrearSolicitudCompraRequestDTO request) {
        validarCrearRequest(request);

        Obra obra = obraRepository.findById(request.getIdObra())
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada"));

        Usuario comprador = usuarioRepository.findById(request.getIdComprador())
                .orElseThrow(() -> new ResourceNotFoundException("Comprador no encontrado"));

        Usuario vendedor = obra.getUsuario();
        if (vendedor == null) {
            throw new BusinessException("Obra sin vendedor");
        }

        if (vendedor.getIdUsuario().equals(comprador.getIdUsuario())) {
            throw new BusinessException("No puedes solicitar tu propia obra");
        }

        if (!"EN_VENTA".equalsIgnoreCase(obra.getEstado())) {
            throw new BusinessException("La obra no esta disponible para solicitud");
        }

        boolean duplicada = solicitudRepository.existsByObraIdObraAndCompradorIdUsuarioAndEstadoSolicitudIn(
                obra.getIdObra(), comprador.getIdUsuario(), List.of("PENDIENTE", "ACEPTADA"));
        if (duplicada) {
            throw new BusinessException("Ya existe una solicitud activa para esta obra");
        }

        long intentosFallidos = solicitudRepository.countByObraIdObraAndCompradorIdUsuarioAndEstadoSolicitudIn(
                obra.getIdObra(),
                comprador.getIdUsuario(),
                List.of("RECHAZADA", "CANCELADA", "EXPIRADA")
        );
        if (intentosFallidos >= MAX_INTENTOS_FALLIDOS_POR_OBRA) {
            throw new BusinessException("Ya alcanzaste el m\u00E1ximo de 3 intentos para esta obra.");
        }

        SolicitudCompraObra solicitud = SolicitudCompraObra.builder()
                .obra(obra)
                .comprador(comprador)
                .vendedor(vendedor)
                .mensajeComprador(normalizarMensajeComprador(request.getMensajeComprador()))
                .estadoSolicitud("PENDIENTE")
                .build();

        SolicitudCompraObra guardada = solicitudRepository.save(solicitud);

        return toDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudCompraDTO> listarRecibidas(Integer vendedorId) {
        validarUsuarioExiste(vendedorId, "Vendedor no encontrado");
        return solicitudRepository.findRecibidas(vendedorId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudCompraDTO> listarEnviadas(Integer compradorId) {
        validarUsuarioExiste(compradorId, "Comprador no encontrado");
        return solicitudRepository.findEnviadas(compradorId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudCompraDTO obtenerDetalle(Integer idSolicitud, Integer actorId) {
        validarIdRequerido(actorId, "actorId es obligatorio");
        validarUsuarioExiste(actorId, "Actor no encontrado");
        SolicitudCompraObra solicitud = obtenerSolicitudDetallada(idSolicitud);
        if (!actorId.equals(solicitud.getComprador().getIdUsuario()) && !actorId.equals(solicitud.getVendedor().getIdUsuario())) {
            throw new BusinessException("No puedes consultar esta solicitud");
        }
        return toDto(solicitud);
    }

    @Override
    @Transactional
    public SolicitudCompraDTO aceptar(Integer idSolicitud, Integer idVendedor) {
        validarIdRequerido(idVendedor, "idVendedor es obligatorio");
        SolicitudCompraObra solicitud = obtenerSolicitudDetallada(idSolicitud);
        validarAccionVendedor(solicitud, idVendedor);

        if (!"PENDIENTE".equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
            throw new BusinessException("Solo se pueden aceptar solicitudes PENDIENTE");
        }

        Obra obra = solicitud.getObra();
        if (!"EN_VENTA".equalsIgnoreCase(obra.getEstado())) {
            throw new BusinessException("La obra no esta disponible para reserva");
        }

        if (solicitudRepository.existsByObraIdObraAndEstadoSolicitud(obra.getIdObra(), "ACEPTADA")) {
            throw new BusinessException("La obra ya tiene una solicitud aceptada");
        }

        List<SolicitudCompraObra> pendientesDeLaObra = solicitudRepository.findByObraIdObraAndEstadoSolicitud(
                obra.getIdObra(),
                "PENDIENTE");

        LocalDateTime expiracion = LocalDateTime.now().plusDays(DIAS_RESERVA);

        solicitud.setEstadoSolicitud("ACEPTADA");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitud.setFechaExpiracionReserva(expiracion);

        obra.setEstado("RESERVADA");
        obraRepository.save(obra);
        solicitudRepository.save(solicitud);

        Carrito carritoExistente = carritoRepository.findByObraIdObra(obra.getIdObra()).orElse(null);
        if (carritoExistente != null && !carritoExistente.getUsuario().getIdUsuario().equals(solicitud.getComprador().getIdUsuario())) {
            throw new BusinessException("La obra ya tiene una reserva activa en carrito");
        }

        if (carritoExistente != null) {
            carritoExistente.setSolicitud(solicitud);
            carritoExistente.setReservadaHasta(expiracion);
            carritoRepository.save(carritoExistente);
        } else {
            carritoRepository.save(Carrito.builder()
                    .usuario(solicitud.getComprador())
                    .obra(obra)
                    .solicitud(solicitud)
                    .reservadaHasta(expiracion)
                    .build());
        }

        solicitudRepository.cerrarPendientesDeObra(
                obra.getIdObra(),
                "PENDIENTE",
                "RECHAZADA",
                LocalDateTime.now(),
                "Solicitud rechazada porque otra solicitud fue aceptada",
                solicitud.getIdSolicitud());

        for (SolicitudCompraObra pendiente : pendientesDeLaObra) {
            if (pendiente.getIdSolicitud().equals(solicitud.getIdSolicitud())) {
                continue;
            }

            notificacionService.crearNotificacionUsuario(
                    pendiente.getComprador().getIdUsuario(),
                    idVendedor,
                    "SOLICITUD_CANCELADA",
                    "Solicitud cancelada",
                    "Tu solicitud para '" + obra.getTitulo() + "' fue cancelada porque se acepto otra solicitud.",
                    null,
                    null
            );
        }

        notificacionService.crearNotificacionUsuario(
                solicitud.getComprador().getIdUsuario(),
                idVendedor,
                "SOLICITUD_ACEPTADA",
                "Solicitud aceptada",
                "Tu solicitud para '" + obra.getTitulo() + "' fue aceptada. Tienes 7 dias para completar el pago.",
                null,
                null
        );

        return toDto(solicitud);
    }

    @Override
    @Transactional
    public SolicitudCompraDTO rechazar(Integer idSolicitud, Integer idVendedor, String motivo) {
        validarIdRequerido(idVendedor, "idVendedor es obligatorio");
        SolicitudCompraObra solicitud = obtenerSolicitudDetallada(idSolicitud);
        validarAccionVendedor(solicitud, idVendedor);

        if (!"PENDIENTE".equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
            throw new BusinessException("Solo se pueden rechazar solicitudes PENDIENTE");
        }

        solicitud.setEstadoSolicitud("RECHAZADA");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        String motivoNormalizado = normalizarTextoOpcional(motivo);
        solicitud.setMotivoRechazo(motivoNormalizado);
        solicitudRepository.save(solicitud);

        notificacionService.crearNotificacionUsuario(
                solicitud.getComprador().getIdUsuario(),
                idVendedor,
                "SOLICITUD_RECHAZADA",
                "Solicitud rechazada",
                construirMensajeRechazo(solicitud.getObra().getTitulo(), motivoNormalizado),
                null,
                null
        );

        return toDto(solicitud);
    }

    @Override
    @Transactional
    public SolicitudCompraDTO cancelar(Integer idSolicitud, Integer idComprador) {
        validarIdRequerido(idComprador, "idComprador es obligatorio");
        SolicitudCompraObra solicitud = obtenerSolicitudDetallada(idSolicitud);
        if (!idComprador.equals(solicitud.getComprador().getIdUsuario())) {
            throw new BusinessException("Solo el comprador puede cancelar su solicitud");
        }

        if (!List.of("PENDIENTE", "ACEPTADA").contains(solicitud.getEstadoSolicitud().toUpperCase())) {
            throw new BusinessException("No se puede cancelar la solicitud en estado " + solicitud.getEstadoSolicitud());
        }

        solicitud.setEstadoSolicitud("CANCELADA");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        Carrito carrito = carritoRepository.findByObraIdObra(solicitud.getObra().getIdObra()).orElse(null);
        if (carrito != null) {
            carritoRepository.delete(carrito);
            solicitud.getObra().setEstado("EN_VENTA");
            obraRepository.save(solicitud.getObra());
        }

        String tituloObra = solicitud.getObra() != null ? solicitud.getObra().getTitulo() : "la obra";
        notificacionService.crearNotificacionSistema(
                idComprador,
                "SOLICITUD_CANCELADA",
                "Solicitud cancelada",
                "Cancelaste tu solicitud para '" + tituloObra + "'.",
                null,
                null
        );

        if (solicitud.getVendedor() != null) {
            notificacionService.crearNotificacionUsuario(
                    solicitud.getVendedor().getIdUsuario(),
                    idComprador,
                    "SOLICITUD_CANCELADA",
                    "Solicitud cancelada",
                    "El comprador cancelo la solicitud para '" + tituloObra + "'.",
                    null,
                    null
            );
        }

        return toDto(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPendientesUsuario(Integer usuarioId) {
        validarUsuarioExiste(usuarioId, "Usuario no encontrado");
        return solicitudRepository.contarPendientesDeUsuario(usuarioId);
    }

    @Override
    @Transactional
    public int expirarReservasVencidas() {
        return carritoService.limpiarReservasVencidas();
    }

    private SolicitudCompraObra obtenerSolicitudDetallada(Integer idSolicitud) {
        return solicitudRepository.findByIdDetallada(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
    }

    private void validarAccionVendedor(SolicitudCompraObra solicitud, Integer idVendedor) {
        validarIdRequerido(idVendedor, "idVendedor es obligatorio");
        if (!idVendedor.equals(solicitud.getVendedor().getIdUsuario())) {
            throw new BusinessException("Solo el vendedor puede realizar esta accion");
        }
        if (solicitud.getObra() == null) {
            throw new BusinessException("La obra asociada ya no existe");
        }
    }

    private void validarCrearRequest(CrearSolicitudCompraRequestDTO request) {
        if (request == null) {
            throw new BusinessException("El request de solicitud es obligatorio");
        }
        validarIdRequerido(request.getIdObra(), "idObra es obligatorio");
        validarIdRequerido(request.getIdComprador(), "idComprador es obligatorio");
    }

    private void validarIdRequerido(Integer id, String mensaje) {
        if (id == null) {
            throw new BusinessException(mensaje);
        }
    }

    private void validarUsuarioExiste(Integer idUsuario, String mensajeNoEncontrado) {
        validarIdRequerido(idUsuario, "idUsuario es obligatorio");
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new ResourceNotFoundException(mensajeNoEncontrado);
        }
    }

    private SolicitudCompraDTO toDto(SolicitudCompraObra s) {
        return SolicitudCompraDTO.builder()
                .idSolicitud(s.getIdSolicitud())
                .idObra(s.getObra() != null ? s.getObra().getIdObra() : null)
                .tituloObra(s.getObra() != null ? s.getObra().getTitulo() : null)
                .idComprador(s.getComprador() != null ? s.getComprador().getIdUsuario() : null)
                .nombreComprador(s.getComprador() != null ? s.getComprador().getUsuario() : null)
                .fotoComprador(s.getComprador() != null ? s.getComprador().getFotoPerfil() : null)
                .idVendedor(s.getVendedor() != null ? s.getVendedor().getIdUsuario() : null)
                .nombreVendedor(s.getVendedor() != null ? s.getVendedor().getUsuario() : null)
                .fotoVendedor(s.getVendedor() != null ? s.getVendedor().getFotoPerfil() : null)
                .mensajeComprador(s.getMensajeComprador())
                .estadoSolicitud(s.getEstadoSolicitud())
                .fechaCreacion(s.getFechaCreacion())
                .fechaRespuesta(s.getFechaRespuesta())
                .fechaExpiracionReserva(s.getFechaExpiracionReserva())
                .motivoRechazo(s.getMotivoRechazo())
                .build();
    }

    private String normalizarMensajeComprador(String mensaje) {
        return normalizarTextoOpcional(mensaje);
    }

    private String normalizarTextoOpcional(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String construirMensajeRechazo(String tituloObra, String motivoRechazo) {
        String mensajeBase = "Tu solicitud para '" + tituloObra + "' fue rechazada.";
        if (motivoRechazo == null) {
            return mensajeBase;
        }
        return mensajeBase + " Motivo: " + motivoRechazo;
    }
}
