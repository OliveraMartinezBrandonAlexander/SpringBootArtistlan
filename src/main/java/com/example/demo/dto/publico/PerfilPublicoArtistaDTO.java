package com.example.demo.dto.publico;

import com.example.demo.dto.ObraDTO;
import com.example.demo.dto.ServicioDTO;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PerfilPublicoArtistaDTO {
    Integer idUsuario;
    String nombreVisible;
    String fotoPerfil;
    String descripcion;
    String ubicacion;
    List<String> categorias;
    List<ObraDTO> obras;
    List<ServicioDTO> servicios;
}
