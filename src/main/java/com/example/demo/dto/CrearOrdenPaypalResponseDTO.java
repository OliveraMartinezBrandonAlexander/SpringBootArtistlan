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
public class CrearOrdenPaypalResponseDTO {

    private Integer idCompra;
    private Integer idObra;
    private String paypalOrderId;
    private String status;
    private String approveLink;
    private BigDecimal monto;
    private String moneda;
}
