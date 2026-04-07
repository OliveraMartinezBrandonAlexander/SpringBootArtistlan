package com.example.demo.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CarritoTotalDTO {
    int cantidad;
    BigDecimal total;
}
