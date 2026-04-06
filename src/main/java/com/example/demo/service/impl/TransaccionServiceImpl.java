package com.example.demo.service.impl;

import com.example.demo.dto.TransaccionResumenDTO;
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

    private void validarUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("idUsuario es obligatorio");
        }

        if (!usuarioRepository.existsById(idUsuario)) {
            throw new EntityNotFoundException("Usuario no encontrado: " + idUsuario);
        }
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
                .idObra(obra != null ? obra.getIdObra() : null)
                .tituloObra(obra != null ? obra.getTitulo() : null)
                .imagenObra(obra != null ? obra.getImagen1() : null)
                .nombreArtista(obtenerNombreUsuario(artista))
                .nombreComprador(obtenerNombreUsuario(comprador))
                .nombreVendedor(obtenerNombreUsuario(vendedor))
                .fechaTransaccion(obtenerFechaTransaccion(compraObra.getFechaCaptura(), compraObra.getFechaCreacion()))
                .precio(compraObra.getMonto())
                .moneda(compraObra.getMoneda())
                .estado(compraObra.getEstado())
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
                .idObra(obra != null ? obra.getIdObra() : null)
                .tituloObra(obra != null ? obra.getTitulo() : null)
                .imagenObra(obra != null ? obra.getImagen1() : null)
                .nombreArtista(obtenerNombreUsuario(artista))
                .nombreComprador(obtenerNombreUsuario(comprador))
                .nombreVendedor(obtenerNombreUsuario(vendedor))
                .fechaTransaccion(compraCarrito != null
                        ? obtenerFechaTransaccion(compraCarrito.getFechaCaptura(), compraCarrito.getFechaCreacion())
                        : null)
                .precio(detalle.getPrecioUnitario())
                .moneda(compraCarrito != null ? compraCarrito.getMoneda() : null)
                .estado(compraCarrito != null ? compraCarrito.getEstado() : null)
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
}
