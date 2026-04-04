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
public class CapturarOrdenPaypalCarritoResponseDTO {

    private Integer idCompraCarrito;
    private String paypalOrderId;
    private String paypalCaptureId;
    private String status;
    private Integer totalObras;
    private boolean obrasVendidas;
    private BigDecimal montoTotal;
    private String moneda;
}
