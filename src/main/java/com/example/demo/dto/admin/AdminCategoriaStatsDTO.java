package com.example.demo.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminCategoriaStatsDTO {
    Integer idCategoria;
    String categoria;
    long total;
}
