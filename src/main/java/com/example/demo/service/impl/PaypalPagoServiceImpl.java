package com.example.demo.service.impl;

import com.example.demo.dto.CapturarOrdenPaypalResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalResponseDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.CompraObraRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.service.NotificacionService;
import com.example.demo.service.ObraService;
import com.example.demo.service.PaypalPagoService;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

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
    private final ObraRepository obraRepository;
    private final ObraService obraService;
    private final NotificacionService notificacionService;

    @Override
    public CrearOrdenPaypalResponseDTO crearOrdenParaObra(Integer idObra, Integer compradorId) {
        Carrito carrito = carritoRepository.findByUsuarioIdUsuarioAndObraIdObra(compradorId, idObra)
                .orElseThrow(() -> new BusinessException("La obra no está reservada para este comprador"));

        Obra obra = carrito.getObra();
        SolicitudCompraObra solicitud = carrito.getSolicitud();

        validarFlujoReserva(obra, solicitud, compradorId, carrito.getReservadaHasta());

        BigDecimal monto = obtenerMontoDesdeObra(obra);

        try {
            OrdersCreateRequest request = new OrdersCreateRequest();
            request.prefer("return=representation");
            request.requestBody(construirOrdenRequest(obra, monto));

            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            CompraObra compra = CompraObra.builder()
                    .obra(obra)
                    .comprador(solicitud.getComprador())
                    .vendedor(solicitud.getVendedor())
                    .solicitud(solicitud)
                    .monto(monto)
                    .moneda(MONEDA)
                    .estado(ESTADO_CREADA)
                    .paypalOrderId(order.id())
                    .build();

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
            throw new RuntimeException("Error al crear la orden en PayPal", e);
        }
    }

    @Override
    @Transactional
    public CapturarOrdenPaypalResponseDTO capturarOrden(String paypalOrderId) {
        CompraObra compra = compraObraRepository.findByPaypalOrderId(paypalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe compra para esa orden PayPal"));

        if (ESTADO_CAPTURADA.equalsIgnoreCase(compra.getEstado())) {
            throw new BusinessException("La orden ya fue capturada");
        }

        Carrito carrito = carritoRepository.findByUsuarioIdUsuarioAndObraIdObra(
                        compra.getComprador().getIdUsuario(),
                        compra.getObra().getIdObra())
                .orElseThrow(() -> new BusinessException("La reserva ya no está activa"));

        validarFlujoReserva(compra.getObra(), compra.getSolicitud(), compra.getComprador().getIdUsuario(), carrito.getReservadaHasta());

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
            compra.setPaypalCaptureId(captureId);
            compra.setEstado(ESTADO_CAPTURADA);
            compra.setFechaCaptura(LocalDateTime.now());
            compraObraRepository.save(compra);

            compra.getSolicitud().setEstadoSolicitud("PAGADA");
            compra.getSolicitud().setFechaRespuesta(LocalDateTime.now());

            obraService.marcarComoVendida(compra.getObra().getIdObra());
            carritoRepository.delete(carrito);

            notificacionService.crearNotificacionUsuario(
                    compra.getVendedor().getIdUsuario(),
                    compra.getComprador().getIdUsuario(),
                    "OBRA_VENDIDA",
                    "Obra vendida",
                    "Tu obra '" + compra.getObra().getTitulo() + "' fue pagada correctamente.",
                    "OBRA",
                    compra.getObra().getIdObra()
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
            throw new RuntimeException("Error al capturar la orden en PayPal", e);
        }
    }

    private void validarFlujoReserva(Obra obra, SolicitudCompraObra solicitud, Integer compradorId, LocalDateTime reservadaHasta) {
        if (obra == null || solicitud == null) {
            throw new BusinessException("Reserva inválida");
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
            throw new BusinessException("La reserva ya expiró");
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
}
