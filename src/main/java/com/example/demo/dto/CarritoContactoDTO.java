package com.example.demo.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CarritoContactoDTO {
    Integer idUsuarioComprador;
    Integer idObra;
    Integer idVendedor;
    String nombreVendedor;
    String usuarioVendedor;
    String correoVendedor;
    String telefonoVendedor;
    String redesVendedor;
    String fotoPerfilVendedor;
}
