package com.example.demo.dto.moderacion;

import com.example.demo.enums.AccionResolucionReporte;
import com.example.demo.enums.EstadoCuenta;
import com.example.demo.enums.EstadoModeracion;
import com.example.demo.enums.EstadoReporte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaModeracionDTO {

    private Boolean success;
    private String message;
    private Integer idReporte;
    private EstadoReporte estadoReporte;
    private AccionResolucionReporte accionEjecutada;
    private EstadoModeracion estadoModeracionContenido;
    private EstadoCuenta estadoCuentaUsuario;
    private LocalDateTime fecha;
}
