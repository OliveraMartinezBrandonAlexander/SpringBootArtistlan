package com.example.demo.service.impl;

import com.example.demo.dto.CapturarOrdenPaypalCarritoResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalCarritoResponseDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.Carrito;
import com.example.demo.model.CompraCarrito;
import com.example.demo.model.CompraCarritoDetalle;
import com.example.demo.model.Obra;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.CompraCarritoRepository;
import com.example.demo.service.NotificacionService;
import com.example.demo.service.ObraService;
import com.example.demo.service.PaypalCarritoService;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.AmountWithBreakdown;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.Capture;
import com.paypal.orders.LinkDescription;
import com.paypal.orders.Order;
import com.paypal.orders.OrderActionRequest;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.PurchaseUnit;
import com.paypal.orders.PurchaseUnitRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaypalCarritoServiceImpl implements PaypalCarritoService {

    private static final String ESTADO_CREADA = "CREADA";
    private static final String ESTADO_CAPTURADA = "CAPTURADA";
    private static final String ESTADO_ERROR = "ERROR";
    private static final String MONEDA = "MXN";

    private final PayPalHttpClient payPalHttpClient;
    private final CarritoRepository carritoRepository;
    private final CompraCarritoRepository compraCarritoRepository;
    private final ObraService obraService;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public CrearOrdenPaypalCarritoResponseDTO crearOrdenParaCarrito(Integer idUsuario) {
        limpiarComprasPendientesNoCapturadas(idUsuario);

        List<Carrito> itemsCarrito = carritoRepository.findByUsuarioId(idUsuario);
        if (itemsCarrito.isEmpty()) {
            throw new BusinessException("El carrito esta vacio");
        }

        List<CompraCarritoDetalle> detalles = new ArrayList<>();
        BigDecimal montoTotal = BigDecimal.ZERO;

        for (Carrito item : itemsCarrito) {
            validarReserva(item, idUsuario);

            Obra obra = item.getObra();
            BigDecimal precioUnitario = obtenerMontoDesdeObra(obra);
            montoTotal = montoTotal.add(precioUnitario);

            detalles.add(CompraCarritoDetalle.builder()
                    .obra(obra)
                    .vendedor(obra.getUsuario())
                    .solicitud(item.getSolicitud())
                    .precioUnitario(precioUnitario)
                    .build());
        }

        try {
            OrdersCreateRequest request = new OrdersCreateRequest();
            request.prefer("return=representation");
            request.requestBody(construirOrdenRequest(itemsCarrito, montoTotal));

            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            CompraCarrito compraCarrito = CompraCarrito.builder()
                    .comprador(itemsCarrito.get(0).getUsuario())
                    .montoTotal(montoTotal)
                    .moneda(MONEDA)
                    .estado(ESTADO_CREADA)
                    .paypalOrderId(order.id())
                    .build();

            List<CompraCarritoDetalle> detallesPersistibles = new ArrayList<>();
            for (CompraCarritoDetalle detalle : detalles) {
                detalle.setCompraCarrito(compraCarrito);
                detallesPersistibles.add(detalle);
            }
            compraCarrito.setDetalles(detallesPersistibles);

            CompraCarrito guardada = compraCarritoRepository.save(compraCarrito);

            return CrearOrdenPaypalCarritoResponseDTO.builder()
                    .idCompraCarrito(guardada.getIdCompraCarrito())
                    .paypalOrderId(order.id())
                    .status(order.status())
                    .approveLink(extraerApproveLink(order))
                    .montoTotal(montoTotal)
                    .moneda(MONEDA)
                    .cantidadObras(detallesPersistibles.size())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Error al crear la orden del carrito en PayPal", e);
        }
    }

    @Override
    @Transactional
    public CapturarOrdenPaypalCarritoResponseDTO capturarOrdenCarrito(String paypalOrderId) {
        CompraCarrito compraCarrito = compraCarritoRepository.findByPaypalOrderIdConDetalles(paypalOrderId)
                .orElseThrow(() -> new EntityNotFoundException("No existe una compra de carrito para la orden PayPal: " + paypalOrderId));

        if (ESTADO_CAPTURADA.equalsIgnoreCase(compraCarrito.getEstado())) {
            throw new BusinessException("La orden del carrito ya fue capturada previamente");
        }

        validarReservaActualAntesDeCapturar(compraCarrito);

        try {
            OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
            request.requestBody(new OrderActionRequest());

            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            if (!"COMPLETED".equalsIgnoreCase(order.status())) {
                compraCarrito.setEstado(ESTADO_ERROR);
                compraCarritoRepository.save(compraCarrito);
                throw new BusinessException("La captura en PayPal no fue completada. Estado actual: " + order.status());
            }

            String captureId = extraerCaptureId(order);
            LocalDateTime fechaCaptura = LocalDateTime.now();
            compraCarrito.setPaypalCaptureId(captureId);
            compraCarrito.setEstado(ESTADO_CAPTURADA);
            compraCarrito.setFechaCaptura(fechaCaptura);
            compraCarritoRepository.save(compraCarrito);

            for (CompraCarritoDetalle detalle : compraCarrito.getDetalles()) {
                detalle.getSolicitud().setEstadoSolicitud("PAGADA");
                detalle.getSolicitud().setFechaRespuesta(fechaCaptura);

                Integer idObra = detalle.getObra().getIdObra();
                obraService.marcarComoVendida(idObra);
                carritoRepository.eliminarTodosPorObra(idObra);

                notificacionService.crearNotificacionUsuario(
                        detalle.getVendedor().getIdUsuario(),
                        compraCarrito.getComprador().getIdUsuario(),
                        "OBRA_VENDIDA",
                        "Obra vendida",
                        "Tu obra '" + detalle.getObra().getTitulo() + "' fue vendida.",
                        "OBRA",
                        idObra
                );
            }

            int totalObras = compraCarrito.getDetalles().size();
            notificacionService.crearNotificacionSistema(
                    compraCarrito.getComprador().getIdUsuario(),
                    "COMPRA_CONFIRMADA",
                    "Compra realizada",
                    construirMensajeCompraCarrito(totalObras),
                    null,
                    null
            );

            return CapturarOrdenPaypalCarritoResponseDTO.builder()
                    .idCompraCarrito(compraCarrito.getIdCompraCarrito())
                    .paypalOrderId(paypalOrderId)
                    .paypalCaptureId(captureId)
                    .status(ESTADO_CAPTURADA)
                    .totalObras(totalObras)
                    .obrasVendidas(true)
                    .montoTotal(compraCarrito.getMontoTotal())
                    .moneda(compraCarrito.getMoneda())
                    .build();

        } catch (IOException e) {
            compraCarrito.setEstado(ESTADO_ERROR);
            compraCarritoRepository.save(compraCarrito);
            throw new RuntimeException("Error al capturar la orden del carrito en PayPal", e);
        }
    }

    private void validarReserva(Carrito item, Integer idUsuario) {
        if (!idUsuario.equals(item.getUsuario().getIdUsuario())) {
            throw new BusinessException("Carrito invalido");
        }
        if (item.getReservadaHasta() != null && item.getReservadaHasta().isBefore(LocalDateTime.now())) {
            throw new BusinessException("La reserva de una obra expiro");
        }
        if (!"RESERVADA".equalsIgnoreCase(item.getObra().getEstado())) {
            throw new BusinessException("Todas las obras del carrito deben estar RESERVADA");
        }
        if (item.getSolicitud() == null || !"ACEPTADA".equalsIgnoreCase(item.getSolicitud().getEstadoSolicitud())) {
            throw new BusinessException("El carrito solo admite solicitudes aceptadas");
        }
    }

    private void validarReservaActualAntesDeCapturar(CompraCarrito compraCarrito) {
        Integer idComprador = compraCarrito.getComprador() != null ? compraCarrito.getComprador().getIdUsuario() : null;
        if (idComprador == null) {
            throw new BusinessException("Compra de carrito invalida");
        }

        for (CompraCarritoDetalle detalle : compraCarrito.getDetalles()) {
            if (detalle.getObra() == null) {
                throw new BusinessException("Compra de carrito invalida: obra faltante");
            }

            Carrito carritoActual = carritoRepository.findByUsuarioIdUsuarioAndObraIdObra(idComprador, detalle.getObra().getIdObra())
                    .orElseThrow(() -> new BusinessException(
                            "La reserva de '" + detalle.getObra().getTitulo() + "' ya no esta activa"));

            validarReserva(carritoActual, idComprador);

            Integer solicitudDetalleId = detalle.getSolicitud() != null ? detalle.getSolicitud().getIdSolicitud() : null;
            Integer solicitudCarritoId = carritoActual.getSolicitud() != null ? carritoActual.getSolicitud().getIdSolicitud() : null;
            if (solicitudDetalleId == null || !solicitudDetalleId.equals(solicitudCarritoId)) {
                throw new BusinessException("La solicitud asociada a '" + detalle.getObra().getTitulo() + "' ya no coincide");
            }
        }
    }

    private OrderRequest construirOrdenRequest(List<Carrito> itemsCarrito, BigDecimal montoTotal) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");
        orderRequest.applicationContext(new ApplicationContext()
                .returnUrl("artistlan://paypal-return")
                .cancelUrl("artistlan://paypal-cancel")
                .userAction("PAY_NOW")
                .shippingPreference("NO_SHIPPING"));

        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .description("Compra de carrito: " + itemsCarrito.size() + " obras")
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(MONEDA)
                        .value(montoTotal.toPlainString()));

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
            throw new BusinessException("La obra no tiene precio configurado. Obra ID: " + obra.getIdObra());
        }

        BigDecimal monto = obra.getPrecio().setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La obra debe tener un precio mayor que cero. Obra ID: " + obra.getIdObra());
        }

        return monto;
    }

    private String construirMensajeCompraCarrito(int totalObras) {
        if (totalObras <= 1) {
            return "Tu compra fue confirmada y la obra ya aparece como pagada.";
        }
        return "Tu compra de carrito fue confirmada para " + totalObras + " obras.";
    }

    private void limpiarComprasPendientesNoCapturadas(Integer idUsuario) {
        List<CompraCarrito> pendientes = compraCarritoRepository.findByCompradorIdUsuarioAndEstadoNot(idUsuario, ESTADO_CAPTURADA);
        if (pendientes.isEmpty()) {
            return;
        }
        compraCarritoRepository.deleteAll(pendientes);
        compraCarritoRepository.flush();
    }
}
