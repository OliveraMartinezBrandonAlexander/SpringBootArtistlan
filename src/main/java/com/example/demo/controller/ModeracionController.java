package com.example.demo.controller;

import com.example.demo.dto.moderacion.ReporteDetalleDTO;
import com.example.demo.dto.moderacion.ReporteResumenDTO;
import com.example.demo.dto.moderacion.ResolverReporteRequestDTO;
import com.example.demo.dto.moderacion.RespuestaModeracionDTO;
import com.example.demo.dto.moderacion.TomarReporteRequestDTO;
import com.example.demo.enums.EstadoReporte;
import com.example.demo.enums.PrioridadReporte;
import com.example.demo.enums.TipoObjetivoReporte;
import com.example.demo.service.ModeracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moderacion/reportes")
@RequiredArgsConstructor
public class ModeracionController {

    private final ModeracionService moderacionService;

    @GetMapping
    public ResponseEntity<List<ReporteResumenDTO>> listarReportes(
            @RequestParam(required = false) Integer idModeradorSolicitante,
            @RequestParam(required = false) EstadoReporte estado,
            @RequestParam(required = false) PrioridadReporte prioridad,
            @RequestParam(required = false) TipoObjetivoReporte tipoObjetivo,
            @RequestParam(required = false) Boolean soloMios) {
        return ResponseEntity.ok(moderacionService.listarReportes(
                idModeradorSolicitante,
                estado,
                prioridad,
                tipoObjetivo,
                soloMios
        ));
    }

    @GetMapping("/{idReporte}")
    public ResponseEntity<ReporteDetalleDTO> obtenerDetalleReporte(
            @PathVariable Integer idReporte,
            @RequestParam(required = false) Integer idModeradorSolicitante) {
        return ResponseEntity.ok(moderacionService.obtenerDetalleReporte(idModeradorSolicitante, idReporte));
    }

    @PostMapping("/{idReporte}/tomar")
    public ResponseEntity<RespuestaModeracionDTO> tomarReporte(@PathVariable Integer idReporte,
                                                               @RequestBody(required = false) TomarReporteRequestDTO request) {
        return ResponseEntity.ok(moderacionService.tomarReporte(idReporte, request));
    }

    @PostMapping("/{idReporte}/resolver")
    public ResponseEntity<RespuestaModeracionDTO> resolverReporte(@PathVariable Integer idReporte,
                                                                  @RequestBody(required = false) ResolverReporteRequestDTO request) {
        return ResponseEntity.ok(moderacionService.resolverReporte(idReporte, request));
    }
}
