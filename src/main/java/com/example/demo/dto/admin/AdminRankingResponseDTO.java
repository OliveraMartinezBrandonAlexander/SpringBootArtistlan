package com.example.demo.dto.admin;

import com.example.demo.enums.AdminDashboardTipo;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminRankingResponseDTO {
    AdminDashboardTipo tipo;
    int limit;
    List<AdminRankingItemDTO> items;
    String mensaje;
}
