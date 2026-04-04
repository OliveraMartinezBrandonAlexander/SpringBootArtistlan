package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearOrdenPaypalCarritoResponseDTO {

    private Integer idCompraCarrito;
    private String paypalOrderId;
    private String status;
    private String approveLink;
    private BigDecimal montoTotal;
    private String moneda;
    private Integer cantidadObras;
}
