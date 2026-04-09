package com.example.demo.dto.notificacion;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class NotificacionDTO {
    Integer idNotificacion;
    String tipoOrigen;
    Integer idUsuarioOrigen;
    String usuarioOrigen;
    String nombreOrigen;
    String fotoOrigen;
    String nombreUsuarioOrigen;
    String fotoPerfilOrigen;
    String tipoNotificacion;
    String rolDestino;
    String titulo;
    String mensaje;
    String referenciaTipo;
    Integer referenciaId;
    Boolean leida;
    LocalDateTime fechaCreacion;
}
