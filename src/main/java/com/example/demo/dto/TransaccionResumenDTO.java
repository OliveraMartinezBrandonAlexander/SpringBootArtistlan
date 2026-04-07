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
public class TransaccionResumenDTO {

    private Integer idTransaccion;
    private String tipoOrigen;
    private Integer idObra;
    private String tituloObra;
    private String imagenObra;
    private String nombreArtista;
    private String nombreComprador;
    private String nombreVendedor;
    private Integer idComprador;
    private Integer idVendedor;
    private String fotoComprador;
    private String fotoVendedor;
    private LocalDateTime fechaTransaccion;
    private BigDecimal precio;
    private String moneda;
    private String estado;
}
