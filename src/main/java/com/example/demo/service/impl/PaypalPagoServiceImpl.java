package com.example.demo.service.impl;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dto.CapturarOrdenPaypalResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalResponseDTO;
import com.example.demo.enums.TipoMetaPersonal;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.CompraObraRepository;
import com.example.demo.repository.SolicitudCompraObraRepository;
import com.example.demo.service.MetaPersonalService;
import com.example.demo.service.NotificacionService;
import com.example.demo.service.ObraService;
import com.example.demo.service.PaypalPagoService;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.http.exceptions.HttpException;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaypalPagoServiceImpl implements PaypalPagoService {

    private static final String ESTADO_CREADA = "CREADA";
    private static final String ESTADO_CAPTURADA = "CAPTURADA";
    private static final String ESTADO_ERROR = "ERROR";
    private static final String MONEDA = "MXN";

    private final PayPalHttpClient payPalHttpClient;
    private final CarritoRepository carritoRepository;
    private final CompraObraRepository compraObraRepository;
    private final SolicitudCompraObraRepository solicitudCompraObraRepository;
    private final ObraService obraService;
    private final MetaPersonalService metaPersonalService;
    private final NotificacionService notificacionService;

    @Override
    public CrearOrdenPaypalResponseDTO crearOrdenParaObra(Integer idObra, Integer compradorId) {
        Integer idCompradorAutenticado = validarCompradorAutenticado(compradorId);
        Obra obraBuscada = obraService.buscarPorId(idObra)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la obra indicada"));

        Integer idVendedor = obraBuscada.getUsuario() != null ? obraBuscada.getUsuario().getIdUsuario() : null;
        if (idVendedor != null && idVendedor.equals(idCompradorAutenticado)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "No puedes comprar tu propia obra.");
        }

        Carrito carrito = carritoRepository.findDetalleByUsuarioYObra(idCompradorAutenticado, idObra)
                .orElseThrow(() -> new BusinessException("La obra no esta reservada para este comprador"));

        Obra obra = carrito.getObra();
        SolicitudCompraObra solicitud = carrito.getSolicitud();

        validarFlujoReserva(obra, solicitud, idCompradorAutenticado, carrito.getReservadaHasta());
        Optional<CompraObra> compraExistente = Optional.empty();
        if (solicitud.getIdSolicitud() != null) {
            compraExistente = compraObraRepository.findBySolicitudIdSolicitud(solicitud.getIdSolicitud());
            if (compraExistente.isPresent() && ESTADO_CAPTURADA.equalsIgnoreCase(compraExistente.get().getEstado())) {
                throw new BusinessException("La reserva ya fue pagada");
            }
        }

        BigDecimal monto = obtenerMontoDesdeObra(obra);

        try {
            OrdersCreateRequest request = new OrdersCreateRequest();
            request.prefer("return=representation");
            request.requestBody(construirOrdenRequest(obra, monto));

            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            CompraObra compra = prepararCompraParaOrden(compraExistente.orElse(null), obra, solicitud, monto, order.id());

            CompraObra guardada = compraObraRepository.save(compra);

            return CrearOrdenPaypalResponseDTO.builder()
                    .idCompra(guardada.getIdCompra())
                    .idObra(obra.getIdObra())
                    .paypalOrderId(order.id())
                    .status(order.status())
                    .approveLink(extraerApproveLink(order))
                    .monto(monto)
                    .moneda(MONEDA)
                    .build();

        } catch (IOException e) {
            throw construirErrorPaypal("crear la orden", e);
        }
    }

    @Override
    @Transactional
    public CapturarOrdenPaypalResponseDTO capturarOrden(String paypalOrderId) {
        Integer idCompradorAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        CompraObra compra = compraObraRepository.findByPaypalOrderId(paypalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe compra para esa orden PayPal"));

        validarOrdenPerteneceAlComprador(compra, idCompradorAutenticado);
        validarEstadoCapturable(compra.getEstado(), "La orden ya fue capturada", "La orden no esta en un estado valido para captura");

        if (compra.getObra() == null || compra.getObra().getIdObra() == null) {
            throw new ResourceNotFoundException("No existe la obra asociada a la orden PayPal");
        }

        Carrito carrito = carritoRepository.findByUsuarioIdUsuarioAndObraIdObra(
                        idCompradorAutenticado,
                        compra.getObra().getIdObra())
                .orElseThrow(() -> new BusinessException("La reserva ya no esta activa"));

        validarFlujoReserva(compra.getObra(), compra.getSolicitud(), idCompradorAutenticado, carrito.getReservadaHasta());

        try {
            OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
            request.requestBody(new OrderActionRequest());

            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            if (!"COMPLETED".equalsIgnoreCase(order.status())) {
                compra.setEstado(ESTADO_ERROR);
                compraObraRepository.save(compra);
                throw new BusinessException("La captura en PayPal no fue completada: " + order.status());
            }

            String captureId = extraerCaptureId(order);
            LocalDateTime fechaCaptura = LocalDateTime.now();
            compra.setPaypalCaptureId(captureId);
            compra.setEstado(ESTADO_CAPTURADA);
            compra.setFechaCaptura(fechaCaptura);
            compraObraRepository.save(compra);

            Integer idSolicitud = compra.getSolicitud() != null ? compra.getSolicitud().getIdSolicitud() : null;
            if (idSolicitud == null) {
                throw new BusinessException("No se encontro solicitud asociada a la compra");
            }
            marcarSolicitudComoPagada(idSolicitud, fechaCaptura);

            obraService.marcarComoVendida(compra.getObra().getIdObra());
            carritoRepository.delete(carrito);

            notificacionService.crearNotificacionSistema(
                    compra.getComprador().getIdUsuario(),
                    "COMPRA_CONFIRMADA",
                    "Compra realizada",
                    "Tu compra de '" + compra.getObra().getTitulo() + "' fue confirmada.",
                    "TRANSACCION",
                    compra.getIdCompra()
            );

            notificacionService.crearNotificacionUsuario(
                    compra.getVendedor().getIdUsuario(),
                    compra.getComprador().getIdUsuario(),
                    "OBRA_VENDIDA",
                    "Obra vendida",
                    "Tu obra '" + compra.getObra().getTitulo() + "' fue vendida y pagada correctamente.",
                    "OBRA",
                    compra.getObra().getIdObra()
            );

            metaPersonalService.evaluarMetasDelUsuarioPorTipos(
                    compra.getVendedor().getIdUsuario(),
                    EnumSet.of(TipoMetaPersonal.VENTAS, TipoMetaPersonal.INGRESOS)
            );

            return CapturarOrdenPaypalResponseDTO.builder()
                    .idCompra(compra.getIdCompra())
                    .idObra(compra.getObra().getIdObra())
                    .paypalOrderId(paypalOrderId)
                    .paypalCaptureId(captureId)
                    .status(ESTADO_CAPTURADA)
                    .obraVendida(true)
                    .build();

        } catch (IOException e) {
            compra.setEstado(ESTADO_ERROR);
            compraObraRepository.save(compra);
            throw construirErrorPaypal("capturar la orden", e);
        }
    }

    private void validarFlujoReserva(Obra obra, SolicitudCompraObra solicitud, Integer compradorId, LocalDateTime reservadaHasta) {
        if (obra == null || solicitud == null) {
            throw new BusinessException("Reserva invalida");
        }
        if (obra.getUsuario() != null && compradorId.equals(obra.getUsuario().getIdUsuario())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "No puedes comprar tu propia obra.");
        }
        if (!"RESERVADA".equalsIgnoreCase(obra.getEstado())) {
            throw new BusinessException("La obra debe estar reservada para pagar");
        }
        if (!"ACEPTADA".equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
            throw new BusinessException("La solicitud debe estar aceptada");
        }
        if (!compradorId.equals(solicitud.getComprador().getIdUsuario())) {
            throw new BusinessException("El comprador no coincide con la reserva");
        }
        if (reservadaHasta != null && reservadaHasta.isBefore(LocalDateTime.now())) {
            throw new BusinessException("La reserva ya expiro");
        }
    }

    private OrderRequest construirOrdenRequest(Obra obra, BigDecimal monto) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");
        orderRequest.applicationContext(new ApplicationContext()
                .returnUrl("artistlan://paypal-return")
                .cancelUrl("artistlan://paypal-cancel")
                .userAction("PAY_NOW")
                .shippingPreference("NO_SHIPPING"));

        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .description("Compra de obra: " + obra.getTitulo())
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(MONEDA)
                        .value(monto.toPlainString()));

        orderRequest.purchaseUnits(List.of(purchaseUnit));
        return orderRequest;
    }

    private String extraerApproveLink(Order order) {
        if (order.links() == null) {
            return null;
        }

        return order.links().stream()
                .filter(link -> "approve".equalsIgnoreCase(link.rel()))
                .map(LinkDescription::href)
                .findFirst()
                .orElse(null);
    }

    private String extraerCaptureId(Order order) {
        if (order.purchaseUnits() == null || order.purchaseUnits().isEmpty()) {
            return null;
        }

        PurchaseUnit purchaseUnit = order.purchaseUnits().get(0);
        if (purchaseUnit.payments() == null || purchaseUnit.payments().captures() == null || purchaseUnit.payments().captures().isEmpty()) {
            return null;
        }

        Capture capture = purchaseUnit.payments().captures().get(0);
        return capture.id();
    }

    private BigDecimal obtenerMontoDesdeObra(Obra obra) {
        if (obra.getPrecio() == null) {
            throw new BusinessException("La obra no tiene precio configurado");
        }

        BigDecimal monto = obra.getPrecio().setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La obra debe tener un precio mayor a cero");
        }

        return monto;
    }

    private CompraObra prepararCompraParaOrden(CompraObra compraExistente,
                                               Obra obra,
                                               SolicitudCompraObra solicitud,
                                               BigDecimal monto,
                                               String paypalOrderId) {
        if (compraExistente == null) {
            return CompraObra.builder()
                    .obra(obra)
                    .comprador(solicitud.getComprador())
                    .vendedor(solicitud.getVendedor())
                    .solicitud(solicitud)
                    .monto(monto)
                    .moneda(MONEDA)
                    .estado(ESTADO_CREADA)
                    .paypalOrderId(paypalOrderId)
                    .build();
        }

        compraExistente.setObra(obra);
        compraExistente.setComprador(solicitud.getComprador());
        compraExistente.setVendedor(solicitud.getVendedor());
        compraExistente.setSolicitud(solicitud);
        compraExistente.setMonto(monto);
        compraExistente.setMoneda(MONEDA);
        compraExistente.setEstado(ESTADO_CREADA);
        compraExistente.setPaypalOrderId(paypalOrderId);
        compraExistente.setPaypalCaptureId(null);
        compraExistente.setFechaCreacion(LocalDateTime.now());
        compraExistente.setFechaCaptura(null);
        return compraExistente;
    }

    private void marcarSolicitudComoPagada(Integer idSolicitud, LocalDateTime fechaCaptura) {
        solicitudCompraObraRepository.findById(idSolicitud).ifPresentOrElse(solicitud -> {
            solicitud.setEstadoSolicitud("PAGADA");
            solicitud.setFechaRespuesta(fechaCaptura);
            solicitudCompraObraRepository.save(solicitud);
        }, () -> {
            throw new BusinessException("Solicitud no encontrada para finalizar pago: " + idSolicitud);
        });
    }

    private BusinessException construirErrorPaypal(String accion, IOException e) {
        String detalle = e.getMessage();
        if (e instanceof HttpException httpException && httpException.getMessage() != null) {
            detalle = httpException.getMessage();
        }
        if (detalle != null && detalle.length() > 350) {
            detalle = detalle.substring(0, 350) + "...";
        }
        String sufijo = (detalle == null || detalle.isBlank()) ? "" : " Detalle: " + detalle;
        return new BusinessException("No se pudo " + accion + " en PayPal." + sufijo);
    }

    private Integer validarCompradorAutenticado(Integer compradorId) {
        Integer idCompradorAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        if (compradorId != null && !compradorId.equals(idCompradorAutenticado)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "No puedes crear una compra usando otro comprador.");
        }
        return idCompradorAutenticado;
    }

    private void validarOrdenPerteneceAlComprador(CompraObra compra, Integer idCompradorAutenticado) {
        Integer idCompradorOrden = compra.getComprador() != null ? compra.getComprador().getIdUsuario() : null;
        if (idCompradorOrden == null) {
            throw new BusinessException("La orden PayPal no tiene comprador asociado");
        }
        if (!idCompradorOrden.equals(idCompradorAutenticado)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "No puedes capturar una orden PayPal de otro usuario.");
        }
    }

    private void validarEstadoCapturable(String estadoActual, String mensajeYaCapturada, String mensajeEstadoInvalido) {
        if (ESTADO_CAPTURADA.equalsIgnoreCase(estadoActual)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, mensajeYaCapturada);
        }
        if (!ESTADO_CREADA.equalsIgnoreCase(estadoActual)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, mensajeEstadoInvalido);
        }
    }
}


