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

    private final SolicitudCompraObraRepository solicitudRepository;
    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public SolicitudCompraDTO crearSolicitud(CrearSolicitudCompraRequestDTO request) {
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
            throw new BusinessException("La obra no está disponible para solicitud");
        }

        boolean duplicada = solicitudRepository.existsByObraIdObraAndCompradorIdUsuarioAndEstadoSolicitudIn(
                obra.getIdObra(), comprador.getIdUsuario(), List.of("PENDIENTE", "ACEPTADA"));
        if (duplicada) {
            throw new BusinessException("Ya existe una solicitud activa para esta obra");
        }

        SolicitudCompraObra solicitud = SolicitudCompraObra.builder()
                .obra(obra)
                .comprador(comprador)
                .vendedor(vendedor)
                .mensajeComprador(request.getMensajeComprador())
                .estadoSolicitud("PENDIENTE")
                .build();

        SolicitudCompraObra guardada = solicitudRepository.save(solicitud);

        notificacionService.crearNotificacionUsuario(
                vendedor.getIdUsuario(),
                comprador.getIdUsuario(),
                "SOLICITUD_CREADA",
                "Nueva solicitud de compra",
                comprador.getUsuario() + " solicitó la compra de '" + obra.getTitulo() + "'.",
                "SOLICITUD",
                guardada.getIdSolicitud()
        );

        return toDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudCompraDTO> listarRecibidas(Integer vendedorId) {
        return solicitudRepository.findRecibidas(vendedorId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudCompraDTO> listarEnviadas(Integer compradorId) {
        return solicitudRepository.findEnviadas(compradorId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudCompraDTO obtenerDetalle(Integer idSolicitud, Integer actorId) {
        SolicitudCompraObra solicitud = obtenerSolicitudDetallada(idSolicitud);
        if (!actorId.equals(solicitud.getComprador().getIdUsuario()) && !actorId.equals(solicitud.getVendedor().getIdUsuario())) {
            throw new BusinessException("No puedes consultar esta solicitud");
        }
        return toDto(solicitud);
    }

    @Override
    @Transactional
    public SolicitudCompraDTO aceptar(Integer idSolicitud, Integer idVendedor) {
        SolicitudCompraObra solicitud = obtenerSolicitudDetallada(idSolicitud);
        validarAccionVendedor(solicitud, idVendedor);

        if (!"PENDIENTE".equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
            throw new BusinessException("Solo se pueden aceptar solicitudes PENDIENTE");
        }

        Obra obra = solicitud.getObra();
        if (!"EN_VENTA".equalsIgnoreCase(obra.getEstado())) {
            throw new BusinessException("La obra no está disponible para reserva");
        }

        if (solicitudRepository.existsByObraIdObraAndEstadoSolicitud(obra.getIdObra(), "ACEPTADA")) {
            throw new BusinessException("La obra ya tiene una solicitud aceptada");
        }

        LocalDateTime expiracion = LocalDateTime.now().plusDays(DIAS_RESERVA);

        solicitud.setEstadoSolicitud("ACEPTADA");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitud.setFechaExpiracionReserva(expiracion);

        obra.setEstado("RESERVADA");
        obraRepository.save(obra);
        solicitudRepository.save(solicitud);

        carritoRepository.save(Carrito.builder()
                .usuario(solicitud.getComprador())
                .obra(obra)
                .solicitud(solicitud)
                .reservadaHasta(expiracion)
                .build());

        solicitudRepository.cerrarPendientesDeObra(
                obra.getIdObra(),
                "PENDIENTE",
                "RECHAZADA",
                LocalDateTime.now(),
                "Solicitud rechazada porque otra solicitud fue aceptada",
                solicitud.getIdSolicitud());

        notificacionService.crearNotificacionUsuario(
                solicitud.getComprador().getIdUsuario(),
                idVendedor,
                "SOLICITUD_ACEPTADA",
                "Solicitud aceptada",
                "Tu solicitud para '" + obra.getTitulo() + "' fue aceptada. Tienes 7 días para completar el pago.",
                "SOLICITUD",
                solicitud.getIdSolicitud()
        );

        return toDto(solicitud);
    }

    @Override
    @Transactional
    public SolicitudCompraDTO rechazar(Integer idSolicitud, Integer idVendedor, String motivo) {
        SolicitudCompraObra solicitud = obtenerSolicitudDetallada(idSolicitud);
        validarAccionVendedor(solicitud, idVendedor);

        if (!"PENDIENTE".equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
            throw new BusinessException("Solo se pueden rechazar solicitudes PENDIENTE");
        }

        solicitud.setEstadoSolicitud("RECHAZADA");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitud.setMotivoRechazo(motivo);
        solicitudRepository.save(solicitud);

        notificacionService.crearNotificacionUsuario(
                solicitud.getComprador().getIdUsuario(),
                idVendedor,
                "SOLICITUD_RECHAZADA",
                "Solicitud rechazada",
                "Tu solicitud para '" + solicitud.getObra().getTitulo() + "' fue rechazada.",
                "SOLICITUD",
                solicitud.getIdSolicitud()
        );

        return toDto(solicitud);
    }

    @Override
    @Transactional
    public SolicitudCompraDTO cancelar(Integer idSolicitud, Integer idComprador) {
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

        return toDto(solicitud);
    }

    @Override
    @Transactional
    public int expirarReservasVencidas() {
        List<SolicitudCompraObra> vencidas = solicitudRepository.findReservasExpiradas(LocalDateTime.now());
        for (SolicitudCompraObra solicitud : vencidas) {
            solicitud.setEstadoSolicitud("EXPIRADA");
            solicitud.setFechaRespuesta(LocalDateTime.now());
            solicitudRepository.save(solicitud);
            Obra obra = solicitud.getObra();
            if (obra != null && "RESERVADA".equalsIgnoreCase(obra.getEstado())) {
                obra.setEstado("EN_VENTA");
                obraRepository.save(obra);
            }
            carritoRepository.findByObraIdObra(solicitud.getObra().getIdObra()).ifPresent(carritoRepository::delete);

            notificacionService.crearNotificacionSistema(
                    solicitud.getComprador().getIdUsuario(),
                    "RESERVA_EXPIRADA",
                    "Reserva expirada",
                    "La reserva de '" + solicitud.getObra().getTitulo() + "' expiró.",
                    "SOLICITUD",
                    solicitud.getIdSolicitud()
            );
        }

        return vencidas.size();
    }

    private SolicitudCompraObra obtenerSolicitudDetallada(Integer idSolicitud) {
        return solicitudRepository.findByIdDetallada(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
    }

    private void validarAccionVendedor(SolicitudCompraObra solicitud, Integer idVendedor) {
        if (!idVendedor.equals(solicitud.getVendedor().getIdUsuario())) {
            throw new BusinessException("Solo el vendedor puede realizar esta acción");
        }
        if (solicitud.getObra() == null) {
            throw new BusinessException("La obra asociada ya no existe");
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
                .mensajeComprador(s.getMensajeComprador())
                .estadoSolicitud(s.getEstadoSolicitud())
                .fechaCreacion(s.getFechaCreacion())
                .fechaRespuesta(s.getFechaRespuesta())
                .fechaExpiracionReserva(s.getFechaExpiracionReserva())
                .motivoRechazo(s.getMotivoRechazo())
                .build();
    }
}
