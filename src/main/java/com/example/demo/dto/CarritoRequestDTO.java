package com.example.demo.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarritoRequestDTO {
    private Integer idUsuario;
    private Integer idObra;
}
