package com.example.demo.dto.publico;

import com.example.demo.dto.ObraDTO;
import com.example.demo.dto.ServicioDTO;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class PerfilPublicoArtistaDTO {
    Integer idUsuario;
    Integer idUsuarioConsulta;
    Integer idUsuarioConsultado;
    String nombreUsuario;
    String nombreCompleto;
    String nombreVisible;
    String fotoPerfil;
    String descripcion;
    String ubicacion;
    String ubicacionPerfil;
    String redes;
    String redesSociales;
    LocalDate fechaNacimiento;
    String categoria;
    String ocupacion;
    List<String> categorias;
    List<ObraDTO> obras;
    List<ServicioDTO> servicios;
}
