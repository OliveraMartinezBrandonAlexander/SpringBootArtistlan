package com.example.demo.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CarritoTotalDTO {
    int cantidad;
    double total;
}
