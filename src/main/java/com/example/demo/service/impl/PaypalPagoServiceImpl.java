package com.example.demo.service.impl;

import com.example.demo.dto.CapturarOrdenPaypalResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalResponseDTO;
import com.example.demo.model.CompraObra;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.CompraCarritoDetalleRepository;
import com.example.demo.repository.CompraObraRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ObraService;
import com.example.demo.service.PaypalPagoService;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaypalPagoServiceImpl implements PaypalPagoService {

    private static final Logger log = LoggerFactory.getLogger(PaypalPagoServiceImpl.class);
    private static final String ESTADO_CREADA = "CREADA";
    private static final String ESTADO_CAPTURADA = "CAPTURADA";
    private static final String ESTADO_ERROR = "ERROR";
    private static final String ESTADO_EN_VENTA = "En venta";
    private static final String MONEDA = "MXN";

    private final PayPalHttpClient payPalHttpClient;
    private final CarritoRepository carritoRepository;
    private final CompraCarritoDetalleRepository compraCarritoDetalleRepository;
    private final CompraObraRepository compraObraRepository;
    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObraService obraService;

    @Override
    public CrearOrdenPaypalResponseDTO crearOrdenParaObra(Integer idObra, Integer compradorId) {
        Obra obra = obraRepository.findById(idObra)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada con ID: " + idObra));

        Usuario comprador = usuarioRepository.findById(compradorId)
                .orElseThrow(() -> new RuntimeException("Comprador no encontrado con ID: " + compradorId));

        Usuario vendedor = obra.getUsuario();
        if (vendedor == null) {
            throw new IllegalStateException("La obra no tiene vendedor asociado");
        }

        if (Objects.equals(vendedor.getIdUsuario(), comprador.getIdUsuario())) {
            throw new IllegalArgumentException("No puedes comprar tu propia obra");
        }

        if (!estaObraComprable(obra)) {
            throw new IllegalStateException("La obra ya fue comprada previamente");
        }

        BigDecimal monto = obtenerMontoDesdeObra(obra);

        try {
            OrdersCreateRequest request = new OrdersCreateRequest();
            request.prefer("return=representation");
            request.requestBody(construirOrdenRequest(obra, monto));

            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            CompraObra compra = CompraObra.builder()
                    .obra(obra)
                    .comprador(comprador)
                    .vendedor(vendedor)
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
            log.error("Error al crear la orden PayPal para la obra {} y comprador {}", idObra, compradorId, e);
            throw new RuntimeException("Error al crear la orden en PayPal", e);
        }
    }

    @Override
    @Transactional
    public CapturarOrdenPaypalResponseDTO capturarOrden(String paypalOrderId) {
        CompraObra compra = compraObraRepository.findByPaypalOrderId(paypalOrderId)
                .orElseThrow(() -> new RuntimeException("No existe una compra registrada para la orden PayPal: " + paypalOrderId));

        if (ESTADO_CAPTURADA.equalsIgnoreCase(compra.getEstado())) {
            throw new IllegalStateException("La orden ya fue capturada previamente");
        }

        if (compra.getPaypalCaptureId() != null && !compra.getPaypalCaptureId().isBlank()) {
            throw new IllegalStateException("La compra ya tiene un paypalCaptureId registrado");
        }

        Integer idObra = compra.getObra().getIdObra();
        if (!estaObraComprable(compra.getObra())) {
            throw new IllegalStateException("La obra ya tiene una compra capturada");
        }

        try {
            OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
            request.requestBody(new OrderActionRequest());

            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            if (!"COMPLETED".equalsIgnoreCase(order.status())) {
                compra.setEstado(ESTADO_ERROR);
                compraObraRepository.save(compra);
                throw new IllegalStateException("La captura en PayPal no fue completada. Estado actual: " + order.status());
            }

            String captureId = extraerCaptureId(order);
            if (captureId == null || captureId.isBlank()) {
                compra.setEstado(ESTADO_ERROR);
                compraObraRepository.save(compra);
                throw new IllegalStateException("PayPal no devolvio un captureId para la orden capturada");
            }

            compra.setPaypalCaptureId(captureId);
            compra.setEstado(ESTADO_CAPTURADA);
            compra.setFechaCaptura(LocalDateTime.now());
            compraObraRepository.save(compra);

            obraService.marcarComoVendida(idObra);
            carritoRepository.eliminarTodosPorObra(idObra);

            return CapturarOrdenPaypalResponseDTO.builder()
                    .idCompra(compra.getIdCompra())
                    .idObra(idObra)
                    .paypalOrderId(paypalOrderId)
                    .paypalCaptureId(captureId)
                    .status(ESTADO_CAPTURADA)
                    .obraVendida(true)
                    .build();

        } catch (IOException e) {
            compra.setEstado(ESTADO_ERROR);
            compraObraRepository.save(compra);
            log.error("Error al capturar la orden PayPal {}", paypalOrderId, e);
            throw new RuntimeException("Error al capturar la orden en PayPal", e);
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

    private boolean estaObraComprable(Obra obra) {
        if (obra == null || obra.getIdObra() == null) {
            return false;
        }

        String estado = obra.getEstado() != null ? obra.getEstado().trim() : "";
        if (!ESTADO_EN_VENTA.equalsIgnoreCase(estado)) {
            return false;
        }

        return !compraObraRepository.existsByObraIdObraAndEstado(obra.getIdObra(), ESTADO_CAPTURADA)
                && !compraCarritoDetalleRepository.existsByObraIdObraAndCompraCarritoEstado(obra.getIdObra(), ESTADO_CAPTURADA);
    }

    private BigDecimal obtenerMontoDesdeObra(Obra obra) {
        if (obra.getPrecio() == null) {
            throw new IllegalStateException("La obra no tiene precio configurado");
        }

        BigDecimal monto = BigDecimal.valueOf(obra.getPrecio()).setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("La obra debe tener un precio mayor que cero");
        }

        return monto;
    }
}
