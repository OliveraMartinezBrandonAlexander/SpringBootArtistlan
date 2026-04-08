package com.example.demo.service.impl;

import com.example.demo.dto.CarritoContactoDTO;
import com.example.demo.dto.CarritoDTO;
import com.example.demo.dto.CarritoRequestDTO;
import com.example.demo.dto.CarritoTotalDTO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CarritoServiceImpl implements CarritoService {

    private static final String ESTADO_EN_VENTA = "EN_VENTA";
    private static final String ESTADO_RESERVADA = "RESERVADA";
    private static final String ESTADO_ACEPTADA = "ACEPTADA";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final String ESTADO_EXPIRADA = "EXPIRADA";

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObraRepository obraRepository;
    private final SolicitudCompraObraRepository solicitudRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public CarritoDTO agregarAlCarrito(CarritoRequestDTO request) {
        throw new BusinessException("No se permite agregar obras manualmente al carrito. Debe aceptarse una solicitud de compra.");
    }

    @Override
    @Transactional
    public List<CarritoDTO> obtenerCarritoUsuario(Integer idUsuario) {
        validarUsuario(idUsuario);

        List<Carrito> items = carritoRepository.findByUsuarioId(idUsuario);
        List<CarritoDTO> activos = new ArrayList<>();

        for (Carrito item : items) {
            if (debeExpirar(item)) {
                expirarItem(item);
                continue;
            }
            if (!esItemComprable(item)) {
                carritoRepository.delete(item);
                continue;
            }
            activos.add(toDto(item));
        }

        return activos;
    }

    @Override
    @Transactional
    public List<CarritoDTO> listarObrasEnCarrito(Integer idUsuario) {
        return obtenerCarritoUsuario(idUsuario);
    }

    @Override
    @Transactional
    public void eliminarDelCarrito(CarritoRequestDTO request) {
        if (request == null || request.getIdUsuario() == null || request.getIdObra() == null) {
            throw new BusinessException("idUsuario e idObra son obligatorios");
        }

        Carrito carrito = carritoRepository.findDetalleByUsuarioYObra(request.getIdUsuario(), request.getIdObra())
                .orElseThrow(() -> new ResourceNotFoundException("La obra no esta en el carrito del usuario"));

        cancelarReservaPorEliminacionDeCarrito(carrito);
    }

    @Override
    @Transactional
    public void limpiarCarritoUsuario(Integer idUsuario) {
        validarUsuario(idUsuario);
        List<Carrito> items = carritoRepository.findByUsuarioId(idUsuario);
        for (Carrito item : items) {
            cancelarReservaPorEliminacionDeCarrito(item);
        }
    }

    @Override
    @Transactional
    public CarritoTotalDTO obtenerTotal(Integer idUsuario) {
        List<CarritoDTO> items = obtenerCarritoUsuario(idUsuario);
        BigDecimal total = items.stream()
                .map(CarritoDTO::getPrecio)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarritoTotalDTO.builder()
                .cantidad(items.size())
                .total(total)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CarritoContactoDTO obtenerContactoVendedor(Integer idUsuario, Integer idObra) {
        validarUsuario(idUsuario);
        if (idObra == null) {
            throw new BusinessException("idObra es obligatorio");
        }

        Carrito carrito = carritoRepository.findDetalleByUsuarioYObra(idUsuario, idObra)
                .orElseThrow(() -> new ResourceNotFoundException("La obra no esta en el carrito del usuario"));

        Obra obra = carrito.getObra();
        if (obra == null || obra.getUsuario() == null) {
            throw new ResourceNotFoundException("No se encontro informacion de contacto del vendedor");
        }

        Usuario vendedor = obra.getUsuario();
        return CarritoContactoDTO.builder()
                .idUsuarioComprador(idUsuario)
                .idObra(idObra)
                .idVendedor(vendedor.getIdUsuario())
                .nombreVendedor(vendedor.getNombreCompleto())
                .usuarioVendedor(vendedor.getUsuario())
                .correoVendedor(vendedor.getCorreo())
                .telefonoVendedor(vendedor.getTelefono())
                .redesVendedor(vendedor.getRedesSociales())
                .fotoPerfilVendedor(vendedor.getFotoPerfil())
                .build();
    }

    @Override
    @Transactional
    public int limpiarReservasVencidas() {
        List<Carrito> vencidas = carritoRepository.findReservasVencidas(LocalDateTime.now());
        int total = 0;
        for (Carrito carrito : vencidas) {
            expirarItem(carrito);
            total++;
        }
        return total;
    }

    private boolean debeExpirar(Carrito item) {
        return item.getReservadaHasta() != null && item.getReservadaHasta().isBefore(LocalDateTime.now());
    }

    private boolean esItemComprable(Carrito item) {
        if (item.getObra() == null || item.getSolicitud() == null) {
            return false;
        }
        boolean obraReservada = ESTADO_RESERVADA.equalsIgnoreCase(item.getObra().getEstado());
        boolean solicitudAceptada = ESTADO_ACEPTADA.equalsIgnoreCase(item.getSolicitud().getEstadoSolicitud());
        return obraReservada && solicitudAceptada;
    }

    private void expirarItem(Carrito carrito) {
        SolicitudCompraObra solicitud = carrito.getSolicitud();
        Obra obra = carrito.getObra();

        if (solicitud != null && ESTADO_ACEPTADA.equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
            solicitud.setEstadoSolicitud(ESTADO_EXPIRADA);
            solicitud.setFechaRespuesta(LocalDateTime.now());
            solicitudRepository.save(solicitud);
        }

        if (obra != null && ESTADO_RESERVADA.equalsIgnoreCase(obra.getEstado())) {
            obra.setEstado(ESTADO_EN_VENTA);
            obraRepository.save(obra);
        }

        notificarReservaExpirada(carrito);
        carritoRepository.delete(carrito);
    }

    private void cancelarReservaPorEliminacionDeCarrito(Carrito carrito) {
        SolicitudCompraObra solicitud = carrito.getSolicitud();
        Obra obra = carrito.getObra();

        if (solicitud != null && ESTADO_ACEPTADA.equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
            solicitud.setEstadoSolicitud(ESTADO_CANCELADA);
            solicitud.setFechaRespuesta(LocalDateTime.now());
            solicitud.setMotivoRechazo("Cancelada por comprador desde carrito");
            solicitudRepository.save(solicitud);
        }

        if (obra != null && ESTADO_RESERVADA.equalsIgnoreCase(obra.getEstado())) {
            obra.setEstado(ESTADO_EN_VENTA);
            obraRepository.save(obra);
        }

        notificarCancelacionDesdeCarrito(carrito);
        carritoRepository.delete(carrito);
    }

    private void notificarReservaExpirada(Carrito carrito) {
        if (carrito.getObra() == null || carrito.getUsuario() == null) {
            return;
        }

        Obra obra = carrito.getObra();
        String tituloObra = obra.getTitulo() != null ? obra.getTitulo() : "la obra";

        notificacionService.crearNotificacionSistema(
                carrito.getUsuario().getIdUsuario(),
                "RESERVA_EXPIRADA",
                "Reserva expirada",
                "La reserva de '" + tituloObra + "' expiro y la obra volvio a En venta.",
                null,
                null
        );

        if (obra.getUsuario() != null) {
            notificacionService.crearNotificacionSistema(
                    obra.getUsuario().getIdUsuario(),
                    "RESERVA_LIBERADA",
                    "Reserva liberada",
                    "La reserva de '" + tituloObra + "' expiro y la obra volvio a En venta.",
                    "OBRA",
                    obra.getIdObra()
            );
        }
    }

    private void notificarCancelacionDesdeCarrito(Carrito carrito) {
        if (carrito.getObra() == null || carrito.getUsuario() == null) {
            return;
        }
        Obra obra = carrito.getObra();
        String tituloObra = obra.getTitulo() != null ? obra.getTitulo() : "la obra";

        notificacionService.crearNotificacionSistema(
                carrito.getUsuario().getIdUsuario(),
                "SOLICITUD_CANCELADA",
                "Solicitud cancelada",
                "Quitaste '" + tituloObra + "' del carrito. La reserva fue liberada.",
                null,
                null
        );

        if (obra.getUsuario() != null) {
            notificacionService.crearNotificacionSistema(
                    obra.getUsuario().getIdUsuario(),
                    "RESERVA_LIBERADA",
                    "Reserva liberada",
                    "El comprador libero la reserva de '" + tituloObra + "' al quitarla del carrito.",
                    "OBRA",
                    obra.getIdObra()
            );
        }
    }

    private void validarUsuario(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + idUsuario));
        if (usuario.getIdUsuario() == null) {
            throw new ResourceNotFoundException("Usuario invalido");
        }
    }

    private CarritoDTO toDto(Carrito carrito) {
        Obra obra = carrito.getObra();
        return CarritoDTO.builder()
                .idCarrito(carrito.getIdCarrito())
                .idUsuario(carrito.getUsuario().getIdUsuario())
                .idObra(obra.getIdObra())
                .idSolicitud(carrito.getSolicitud() != null ? carrito.getSolicitud().getIdSolicitud() : null)
                .idArtista(obra.getUsuario() != null ? obra.getUsuario().getIdUsuario() : null)
                .tituloObra(obra.getTitulo())
                .descripcion(obra.getDescripcion())
                .estadoObra(estadoParaMostrar(obra.getEstado()))
                .precio(obra.getPrecio())
                .imagen1(obra.getImagen1())
                .tecnicas(obra.getTecnicas())
                .medidas(obra.getMedidas())
                .nombreAutor(obra.getUsuario() != null ? obra.getUsuario().getUsuario() : null)
                .fotoPerfilAutor(obra.getUsuario() != null ? obra.getUsuario().getFotoPerfil() : null)
                .contactoVendedor(obra.getUsuario() != null ? obra.getUsuario().getCorreo() : null)
                .fechaAgregado(carrito.getFechaAgregado())
                .reservadaHasta(carrito.getReservadaHasta())
                .build();
    }

    private String estadoParaMostrar(String estadoOriginal) {
        if (estadoOriginal == null) {
            return null;
        }
        String sinAcentos = Normalizer.normalize(estadoOriginal, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String estado = sinAcentos.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        return switch (estado) {
            case "EN_VENTA" -> "En venta";
            case "EN_EXHIBICION" -> "En exhibici\u00F3n";
            case "RESERVADA" -> "Reservada";
            case "VENDIDA" -> "Vendida";
            default -> estadoOriginal;
        };
    }
}
