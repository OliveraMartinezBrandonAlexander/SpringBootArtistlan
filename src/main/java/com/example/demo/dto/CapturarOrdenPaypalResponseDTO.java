package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapturarOrdenPaypalResponseDTO {

    private Integer idCompra;
    private Integer idObra;
    private String paypalOrderId;
    private String paypalCaptureId;
    private String status;
    private boolean obraVendida;
}
