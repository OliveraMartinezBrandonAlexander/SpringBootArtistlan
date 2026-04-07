package com.example.demo.controller;

import com.example.demo.dto.notificacion.ContadorNoLeidasDTO;
import com.example.demo.dto.notificacion.NotificacionDTO;
import com.example.demo.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping("/{idUsuario}")
    public ResponseEntity<List<NotificacionDTO>> listar(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(notificacionService.listarPorUsuario(idUsuario));
    }

    @GetMapping("/{idUsuario}/{idNotificacion}")
    public ResponseEntity<NotificacionDTO> detalle(@PathVariable Integer idUsuario,
                                                   @PathVariable Integer idNotificacion) {
        return ResponseEntity.ok(notificacionService.obtenerDetalle(idUsuario, idNotificacion));
    }

    @PatchMapping("/{idUsuario}/{idNotificacion}/leida")
    public ResponseEntity<NotificacionDTO> marcarLeida(@PathVariable Integer idUsuario,
                                                       @PathVariable Integer idNotificacion) {
        return ResponseEntity.ok(notificacionService.marcarLeida(idUsuario, idNotificacion));
    }

    @PatchMapping("/{idUsuario}/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@PathVariable Integer idUsuario) {
        notificacionService.marcarTodasLeidas(idUsuario);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idUsuario}/{idNotificacion}")
    public ResponseEntity<Void> eliminarLogica(@PathVariable Integer idUsuario,
                                               @PathVariable Integer idNotificacion) {
        notificacionService.eliminarLogicamente(idUsuario, idNotificacion);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{idUsuario}/contador-no-leidas")
    public ResponseEntity<ContadorNoLeidasDTO> contador(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(ContadorNoLeidasDTO.builder()
                .noLeidas(notificacionService.contarNoLeidas(idUsuario))
                .build());
    }
}
