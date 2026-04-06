package com.example.demo.service.impl;

import com.example.demo.dto.CapturarOrdenPaypalCarritoResponseDTO;
import com.example.demo.dto.CrearOrdenPaypalCarritoResponseDTO;
import com.example.demo.model.Carrito;
import com.example.demo.model.CompraCarrito;
import com.example.demo.model.CompraCarritoDetalle;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.CompraCarritoDetalleRepository;
import com.example.demo.repository.CompraCarritoRepository;
import com.example.demo.repository.CompraObraRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ObraService;
import com.example.demo.service.PaypalCarritoService;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaypalCarritoServiceImpl implements PaypalCarritoService {

    private static final Logger log = LoggerFactory.getLogger(PaypalCarritoServiceImpl.class);
    private static final String ESTADO_CREADA = "CREADA";
    private static final String ESTADO_CAPTURADA = "CAPTURADA";
    private static final String ESTADO_ERROR = "ERROR";
    private static final String ESTADO_EN_VENTA = "En venta";
    private static final String MONEDA = "MXN";

    private final PayPalHttpClient payPalHttpClient;
    private final CarritoRepository carritoRepository;
    private final CompraCarritoRepository compraCarritoRepository;
    private final CompraCarritoDetalleRepository compraCarritoDetalleRepository;
    private final CompraObraRepository compraObraRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObraService obraService;

    @Override
    @Transactional
    public CrearOrdenPaypalCarritoResponseDTO crearOrdenParaCarrito(Integer idUsuario) {
        Usuario comprador = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuario));

        List<Carrito> itemsCarrito = obtenerItemsComprablesDelCarrito(idUsuario);
        if (itemsCarrito.isEmpty()) {
            throw new IllegalStateException("El carrito esta vacio");
        }

        List<CompraCarritoDetalle> detalles = new ArrayList<>();
        BigDecimal montoTotal = BigDecimal.ZERO;

        for (Carrito item : itemsCarrito) {
            Obra obra = item.getObra();
            validarObraDisponibleParaCheckout(comprador, obra);

            BigDecimal precioUnitario = obtenerMontoDesdeObra(obra);
            montoTotal = montoTotal.add(precioUnitario);

            detalles.add(CompraCarritoDetalle.builder()
                    .obra(obra)
                    .vendedor(obra.getUsuario())
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
                    .comprador(comprador)
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
            log.error("Error al crear la orden PayPal para el carrito del usuario {}", idUsuario, e);
            throw new RuntimeException("Error al crear la orden del carrito en PayPal", e);
        }
    }

    @Override
    @Transactional
    public CapturarOrdenPaypalCarritoResponseDTO capturarOrdenCarrito(String paypalOrderId) {
        CompraCarrito compraCarrito = compraCarritoRepository.findByPaypalOrderIdConDetalles(paypalOrderId)
                .orElseThrow(() -> new EntityNotFoundException("No existe una compra de carrito para la orden PayPal: " + paypalOrderId));

        if (ESTADO_CAPTURADA.equalsIgnoreCase(compraCarrito.getEstado())) {
            throw new IllegalStateException("La orden del carrito ya fue capturada previamente");
        }

        if (compraCarrito.getPaypalCaptureId() != null && !compraCarrito.getPaypalCaptureId().isBlank()) {
            throw new IllegalStateException("La compra del carrito ya tiene un paypalCaptureId registrado");
        }

        if (compraCarrito.getDetalles() == null || compraCarrito.getDetalles().isEmpty()) {
            throw new IllegalStateException("La compra del carrito no tiene obras asociadas");
        }

        for (CompraCarritoDetalle detalle : compraCarrito.getDetalles()) {
            validarObraSigueDisponibleParaCaptura(compraCarrito.getComprador(), detalle.getObra());
        }

        try {
            OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
            request.requestBody(new OrderActionRequest());

            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            if (!"COMPLETED".equalsIgnoreCase(order.status())) {
                compraCarrito.setEstado(ESTADO_ERROR);
                compraCarritoRepository.save(compraCarrito);
                throw new IllegalStateException("La captura en PayPal no fue completada. Estado actual: " + order.status());
            }

            String captureId = extraerCaptureId(order);
            if (captureId == null || captureId.isBlank()) {
                compraCarrito.setEstado(ESTADO_ERROR);
                compraCarritoRepository.save(compraCarrito);
                throw new IllegalStateException("PayPal no devolvio un captureId para la orden capturada");
            }

            compraCarrito.setPaypalCaptureId(captureId);
            compraCarrito.setEstado(ESTADO_CAPTURADA);
            compraCarrito.setFechaCaptura(LocalDateTime.now());
            compraCarritoRepository.save(compraCarrito);

            for (CompraCarritoDetalle detalle : compraCarrito.getDetalles()) {
                Integer idObra = detalle.getObra().getIdObra();
                obraService.marcarComoVendida(idObra);
                carritoRepository.eliminarTodosPorObra(idObra);
            }

            return CapturarOrdenPaypalCarritoResponseDTO.builder()
                    .idCompraCarrito(compraCarrito.getIdCompraCarrito())
                    .paypalOrderId(paypalOrderId)
                    .paypalCaptureId(captureId)
                    .status(ESTADO_CAPTURADA)
                    .totalObras(compraCarrito.getDetalles().size())
                    .obrasVendidas(true)
                    .montoTotal(compraCarrito.getMontoTotal())
                    .moneda(compraCarrito.getMoneda())
                    .build();

        } catch (IOException e) {
            compraCarrito.setEstado(ESTADO_ERROR);
            compraCarritoRepository.save(compraCarrito);
            log.error("Error al capturar la orden PayPal del carrito {}", paypalOrderId, e);
            throw new RuntimeException("Error al capturar la orden del carrito en PayPal", e);
        }
    }

    private void validarObraDisponibleParaCheckout(Usuario comprador, Obra obra) {
        Usuario vendedor = obra.getUsuario();
        if (vendedor == null || vendedor.getIdUsuario() == null) {
            throw new IllegalStateException("La obra " + obra.getIdObra() + " no tiene vendedor asociado");
        }

        if (Objects.equals(vendedor.getIdUsuario(), comprador.getIdUsuario())) {
            throw new IllegalArgumentException("No puedes comprar tu propia obra. Obra ID: " + obra.getIdObra());
        }

        validarEstadoEnVenta(obra);

        if (compraObraRepository.existsByObraIdObraAndEstado(obra.getIdObra(), ESTADO_CAPTURADA)) {
            throw new IllegalStateException("La obra ya fue comprada previamente. Obra ID: " + obra.getIdObra());
        }

        if (compraCarritoDetalleRepository.existsByObraIdObraAndCompraCarritoEstado(obra.getIdObra(), ESTADO_CAPTURADA)) {
            throw new IllegalStateException("La obra ya fue comprada previamente en una compra de carrito. Obra ID: " + obra.getIdObra());
        }
    }

    private void validarObraSigueDisponibleParaCaptura(Usuario comprador, Obra obra) {
        validarObraDisponibleParaCheckout(comprador, obra);

        if (!obraService.estaDisponibleParaVenta(obra.getIdObra())) {
            throw new IllegalStateException("La obra ya no esta disponible para venta. Obra ID: " + obra.getIdObra());
        }
    }

    private void validarEstadoEnVenta(Obra obra) {
        String estado = obra.getEstado() != null ? obra.getEstado().trim() : "";
        if (!ESTADO_EN_VENTA.equalsIgnoreCase(estado)) {
            throw new IllegalStateException("La obra no esta disponible para venta. Obra ID: " + obra.getIdObra());
        }
    }

    private List<Carrito> obtenerItemsComprablesDelCarrito(Integer idUsuario) {
        List<Carrito> itemsCarrito = carritoRepository.findByUsuarioId(idUsuario);
        List<Carrito> itemsDisponibles = itemsCarrito.stream()
                .filter(item -> esObraComprable(item.getObra()))
                .toList();

        List<Carrito> itemsNoDisponibles = itemsCarrito.stream()
                .filter(item -> !esObraComprable(item.getObra()))
                .toList();

        if (!itemsNoDisponibles.isEmpty()) {
            carritoRepository.deleteAll(itemsNoDisponibles);
        }

        return itemsDisponibles;
    }

    private boolean esObraComprable(Obra obra) {
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
            throw new IllegalStateException("La obra no tiene precio configurado. Obra ID: " + obra.getIdObra());
        }

        BigDecimal monto = BigDecimal.valueOf(obra.getPrecio()).setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("La obra debe tener un precio mayor que cero. Obra ID: " + obra.getIdObra());
        }

        return monto;
    }
}
