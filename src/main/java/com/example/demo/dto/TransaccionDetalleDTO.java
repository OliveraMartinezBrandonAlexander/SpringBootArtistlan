package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionDetalleDTO {

    private Integer idTransaccion;
    private String tipoOrigen;
    private String rolUsuario;
    private Integer idCompraCarrito;

    private Integer idObra;
    private Integer idSolicitud;
    private String tituloObra;
    private String imagenObra;

    private BigDecimal monto;
    private String moneda;
    private String estadoPago;
    private String paypalOrderId;
    private String paypalCaptureId;

    private LocalDateTime fechaCreacionPago;
    private LocalDateTime fechaCapturaPago;
    private LocalDateTime fechaTransaccion;

    private Integer idComprador;
    private String nombreComprador;
    private String usuarioComprador;
    private String fotoComprador;

    private Integer idVendedor;
    private String nombreVendedor;
    private String usuarioVendedor;
    private String fotoVendedor;
}
