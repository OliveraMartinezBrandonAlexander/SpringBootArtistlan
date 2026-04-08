package com.example.demo.service.impl;

import com.example.demo.dto.TransaccionDetalleDTO;
import com.example.demo.dto.TransaccionResumenDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.CompraCarrito;
import com.example.demo.model.CompraCarritoDetalle;
import com.example.demo.model.CompraObra;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CompraCarritoDetalleRepository;
import com.example.demo.repository.CompraObraRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.TransaccionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransaccionServiceImpl implements TransaccionService {

    private static final String TIPO_OBRA_DIRECTA = "OBRA_DIRECTA";
    private static final String TIPO_CARRITO = "CARRITO";
    private static final String ESTADO_TRANSACCION_COMPLETADA = "CAPTURADA";
    private static final String ROL_COMPRA = "COMPRA";
    private static final String ROL_VENTA = "VENTA";

    private final UsuarioRepository usuarioRepository;
    private final CompraObraRepository compraObraRepository;
    private final CompraCarritoDetalleRepository compraCarritoDetalleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResumenDTO> obtenerComprasUsuario(Integer idUsuario) {
        validarUsuario(idUsuario);

        List<TransaccionResumenDTO> transacciones = new ArrayList<>();
        compraObraRepository.findComprasDirectasByCompradorId(idUsuario, ESTADO_TRANSACCION_COMPLETADA)
                .stream()
                .map(this::mapearCompraDirecta)
                .forEach(transacciones::add);

        compraCarritoDetalleRepository.findComprasCarritoByCompradorId(idUsuario, ESTADO_TRANSACCION_COMPLETADA)
                .stream()
                .map(this::mapearCompraCarrito)
                .forEach(transacciones::add);

        return ordenarPorFechaDesc(transacciones);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResumenDTO> obtenerVentasUsuario(Integer idUsuario) {
        validarUsuario(idUsuario);

        List<TransaccionResumenDTO> transacciones = new ArrayList<>();
        compraObraRepository.findVentasDirectasByVendedorId(idUsuario, ESTADO_TRANSACCION_COMPLETADA)
                .stream()
                .map(this::mapearCompraDirecta)
                .forEach(transacciones::add);

        compraCarritoDetalleRepository.findVentasCarritoByVendedorId(idUsuario, ESTADO_TRANSACCION_COMPLETADA)
                .stream()
                .map(this::mapearCompraCarrito)
                .forEach(transacciones::add);

        return ordenarPorFechaDesc(transacciones);
    }

    @Override
    @Transactional(readOnly = true)
    public TransaccionDetalleDTO obtenerDetalleTransaccion(Integer idUsuario, String tipoOrigen, Integer idTransaccion) {
        validarUsuario(idUsuario);
        if (idTransaccion == null) {
            throw new BusinessException("idTransaccion es obligatorio");
        }

        String tipoNormalizado = normalizarTipoOrigen(tipoOrigen);
        return switch (tipoNormalizado) {
            case TIPO_OBRA_DIRECTA -> obtenerDetalleCompraDirecta(idUsuario, idTransaccion);
            case TIPO_CARRITO -> obtenerDetalleCompraCarrito(idUsuario, idTransaccion);
            default -> throw new BusinessException("tipoOrigen invalido. Usa OBRA_DIRECTA o CARRITO");
        };
    }

    private TransaccionDetalleDTO obtenerDetalleCompraDirecta(Integer idUsuario, Integer idTransaccion) {
        CompraObra compraObra = compraObraRepository.findByIdDetallada(idTransaccion)
                .orElseThrow(() -> new ResourceNotFoundException("Transaccion no encontrada"));

        validarActorDeTransaccion(idUsuario, compraObra.getComprador(), compraObra.getVendedor());

        Obra obra = compraObra.getObra();
        Usuario comprador = compraObra.getComprador();
        Usuario vendedor = compraObra.getVendedor();

        return TransaccionDetalleDTO.builder()
                .idTransaccion(compraObra.getIdCompra())
                .tipoOrigen(TIPO_OBRA_DIRECTA)
                .rolUsuario(definirRolUsuario(idUsuario, comprador))
                .idCompraCarrito(null)
                .idObra(obra != null ? obra.getIdObra() : null)
                .idSolicitud(compraObra.getSolicitud() != null ? compraObra.getSolicitud().getIdSolicitud() : null)
                .tituloObra(obra != null ? obra.getTitulo() : null)
                .imagenObra(obra != null ? obra.getImagen1() : null)
                .monto(compraObra.getMonto())
                .moneda(compraObra.getMoneda())
                .estadoPago(compraObra.getEstado())
                .paypalOrderId(compraObra.getPaypalOrderId())
                .paypalCaptureId(compraObra.getPaypalCaptureId())
                .fechaCreacionPago(compraObra.getFechaCreacion())
                .fechaCapturaPago(compraObra.getFechaCaptura())
                .fechaTransaccion(obtenerFechaTransaccion(compraObra.getFechaCaptura(), compraObra.getFechaCreacion()))
                .idComprador(comprador != null ? comprador.getIdUsuario() : null)
                .nombreComprador(obtenerNombreUsuario(comprador))
                .usuarioComprador(obtenerUsuarioLogin(comprador))
                .fotoComprador(comprador != null ? comprador.getFotoPerfil() : null)
                .idVendedor(vendedor != null ? vendedor.getIdUsuario() : null)
                .nombreVendedor(obtenerNombreUsuario(vendedor))
                .usuarioVendedor(obtenerUsuarioLogin(vendedor))
                .fotoVendedor(vendedor != null ? vendedor.getFotoPerfil() : null)
                .build();
    }

    private TransaccionDetalleDTO obtenerDetalleCompraCarrito(Integer idUsuario, Integer idTransaccion) {
        CompraCarritoDetalle detalle = compraCarritoDetalleRepository.findByIdDetallada(idTransaccion)
                .orElseThrow(() -> new ResourceNotFoundException("Transaccion no encontrada"));

        CompraCarrito compraCarrito = detalle.getCompraCarrito();
        Usuario comprador = compraCarrito != null ? compraCarrito.getComprador() : null;
        Usuario vendedor = detalle.getVendedor();

        validarActorDeTransaccion(idUsuario, comprador, vendedor);

        Obra obra = detalle.getObra();
        return TransaccionDetalleDTO.builder()
                .idTransaccion(detalle.getIdDetalle())
                .tipoOrigen(TIPO_CARRITO)
                .rolUsuario(definirRolUsuario(idUsuario, comprador))
                .idCompraCarrito(compraCarrito != null ? compraCarrito.getIdCompraCarrito() : null)
                .idObra(obra != null ? obra.getIdObra() : null)
                .idSolicitud(detalle.getSolicitud() != null ? detalle.getSolicitud().getIdSolicitud() : null)
                .tituloObra(obra != null ? obra.getTitulo() : null)
                .imagenObra(obra != null ? obra.getImagen1() : null)
                .monto(detalle.getPrecioUnitario())
                .moneda(compraCarrito != null ? compraCarrito.getMoneda() : null)
                .estadoPago(compraCarrito != null ? compraCarrito.getEstado() : null)
                .paypalOrderId(compraCarrito != null ? compraCarrito.getPaypalOrderId() : null)
                .paypalCaptureId(compraCarrito != null ? compraCarrito.getPaypalCaptureId() : null)
                .fechaCreacionPago(compraCarrito != null ? compraCarrito.getFechaCreacion() : null)
                .fechaCapturaPago(compraCarrito != null ? compraCarrito.getFechaCaptura() : null)
                .fechaTransaccion(compraCarrito != null
                        ? obtenerFechaTransaccion(compraCarrito.getFechaCaptura(), compraCarrito.getFechaCreacion())
                        : null)
                .idComprador(comprador != null ? comprador.getIdUsuario() : null)
                .nombreComprador(obtenerNombreUsuario(comprador))
                .usuarioComprador(obtenerUsuarioLogin(comprador))
                .fotoComprador(comprador != null ? comprador.getFotoPerfil() : null)
                .idVendedor(vendedor != null ? vendedor.getIdUsuario() : null)
                .nombreVendedor(obtenerNombreUsuario(vendedor))
                .usuarioVendedor(obtenerUsuarioLogin(vendedor))
                .fotoVendedor(vendedor != null ? vendedor.getFotoPerfil() : null)
                .build();
    }

    private void validarUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("idUsuario es obligatorio");
        }

        if (!usuarioRepository.existsById(idUsuario)) {
            throw new EntityNotFoundException("Usuario no encontrado: " + idUsuario);
        }
    }

    private String normalizarTipoOrigen(String tipoOrigen) {
        if (tipoOrigen == null || tipoOrigen.isBlank()) {
            throw new BusinessException("tipoOrigen es obligatorio");
        }
        return tipoOrigen.trim().toUpperCase();
    }

    private void validarActorDeTransaccion(Integer idUsuario, Usuario comprador, Usuario vendedor) {
        Integer idComprador = comprador != null ? comprador.getIdUsuario() : null;
        Integer idVendedor = vendedor != null ? vendedor.getIdUsuario() : null;
        boolean participa = idUsuario.equals(idComprador) || idUsuario.equals(idVendedor);
        if (!participa) {
            throw new BusinessException("No puedes consultar una transaccion ajena");
        }
    }

    private String definirRolUsuario(Integer idUsuario, Usuario comprador) {
        return comprador != null && idUsuario.equals(comprador.getIdUsuario()) ? ROL_COMPRA : ROL_VENTA;
    }

    private List<TransaccionResumenDTO> ordenarPorFechaDesc(List<TransaccionResumenDTO> transacciones) {
        Comparator<LocalDateTime> comparadorFechas = Comparator.nullsLast(Comparator.reverseOrder());
        return transacciones.stream()
                .sorted(Comparator.comparing(TransaccionResumenDTO::getFechaTransaccion, comparadorFechas))
                .toList();
    }

    private TransaccionResumenDTO mapearCompraDirecta(CompraObra compraObra) {
        Obra obra = compraObra.getObra();
        Usuario comprador = compraObra.getComprador();
        Usuario vendedor = compraObra.getVendedor();
        Usuario artista = obra != null && obra.getUsuario() != null ? obra.getUsuario() : vendedor;

        return TransaccionResumenDTO.builder()
                .idTransaccion(compraObra.getIdCompra())
                .tipoOrigen(TIPO_OBRA_DIRECTA)
                .idCompraCarrito(null)
                .idObra(obra != null ? obra.getIdObra() : null)
                .idSolicitud(compraObra.getSolicitud() != null ? compraObra.getSolicitud().getIdSolicitud() : null)
                .tituloObra(obra != null ? obra.getTitulo() : null)
                .imagenObra(obra != null ? obra.getImagen1() : null)
                .nombreArtista(obtenerNombreUsuario(artista))
                .nombreComprador(obtenerNombreUsuario(comprador))
                .usuarioComprador(obtenerUsuarioLogin(comprador))
                .nombreVendedor(obtenerNombreUsuario(vendedor))
                .usuarioVendedor(obtenerUsuarioLogin(vendedor))
                .idComprador(comprador != null ? comprador.getIdUsuario() : null)
                .idVendedor(vendedor != null ? vendedor.getIdUsuario() : null)
                .fotoComprador(comprador != null ? comprador.getFotoPerfil() : null)
                .fotoVendedor(vendedor != null ? vendedor.getFotoPerfil() : null)
                .fechaTransaccion(obtenerFechaTransaccion(compraObra.getFechaCaptura(), compraObra.getFechaCreacion()))
                .fechaCreacionPago(compraObra.getFechaCreacion())
                .fechaCapturaPago(compraObra.getFechaCaptura())
                .precio(compraObra.getMonto())
                .moneda(compraObra.getMoneda())
                .estado(compraObra.getEstado())
                .paypalOrderId(compraObra.getPaypalOrderId())
                .paypalCaptureId(compraObra.getPaypalCaptureId())
                .build();
    }

    private TransaccionResumenDTO mapearCompraCarrito(CompraCarritoDetalle detalle) {
        CompraCarrito compraCarrito = detalle.getCompraCarrito();
        Obra obra = detalle.getObra();
        Usuario comprador = compraCarrito != null ? compraCarrito.getComprador() : null;
        Usuario vendedor = detalle.getVendedor();
        Usuario artista = obra != null && obra.getUsuario() != null ? obra.getUsuario() : vendedor;

        return TransaccionResumenDTO.builder()
                .idTransaccion(detalle.getIdDetalle())
                .tipoOrigen(TIPO_CARRITO)
                .idCompraCarrito(compraCarrito != null ? compraCarrito.getIdCompraCarrito() : null)
                .idObra(obra != null ? obra.getIdObra() : null)
                .idSolicitud(detalle.getSolicitud() != null ? detalle.getSolicitud().getIdSolicitud() : null)
                .tituloObra(obra != null ? obra.getTitulo() : null)
                .imagenObra(obra != null ? obra.getImagen1() : null)
                .nombreArtista(obtenerNombreUsuario(artista))
                .nombreComprador(obtenerNombreUsuario(comprador))
                .usuarioComprador(obtenerUsuarioLogin(comprador))
                .nombreVendedor(obtenerNombreUsuario(vendedor))
                .usuarioVendedor(obtenerUsuarioLogin(vendedor))
                .idComprador(comprador != null ? comprador.getIdUsuario() : null)
                .idVendedor(vendedor != null ? vendedor.getIdUsuario() : null)
                .fotoComprador(comprador != null ? comprador.getFotoPerfil() : null)
                .fotoVendedor(vendedor != null ? vendedor.getFotoPerfil() : null)
                .fechaTransaccion(compraCarrito != null
                        ? obtenerFechaTransaccion(compraCarrito.getFechaCaptura(), compraCarrito.getFechaCreacion())
                        : null)
                .fechaCreacionPago(compraCarrito != null ? compraCarrito.getFechaCreacion() : null)
                .fechaCapturaPago(compraCarrito != null ? compraCarrito.getFechaCaptura() : null)
                .precio(detalle.getPrecioUnitario())
                .moneda(compraCarrito != null ? compraCarrito.getMoneda() : null)
                .estado(compraCarrito != null ? compraCarrito.getEstado() : null)
                .paypalOrderId(compraCarrito != null ? compraCarrito.getPaypalOrderId() : null)
                .paypalCaptureId(compraCarrito != null ? compraCarrito.getPaypalCaptureId() : null)
                .build();
    }

    private LocalDateTime obtenerFechaTransaccion(LocalDateTime fechaCaptura, LocalDateTime fechaCreacion) {
        return fechaCaptura != null ? fechaCaptura : fechaCreacion;
    }

    private String obtenerNombreUsuario(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        String nombreCompleto = usuario.getNombreCompleto();
        if (nombreCompleto != null && !nombreCompleto.isBlank()) {
            return nombreCompleto;
        }

        String nombreUsuario = usuario.getUsuario();
        return nombreUsuario != null && !nombreUsuario.isBlank() ? nombreUsuario : null;
    }

    private String obtenerUsuarioLogin(Usuario usuario) {
        if (usuario == null || usuario.getUsuario() == null || usuario.getUsuario().isBlank()) {
            return null;
        }
        return usuario.getUsuario();
    }
}
