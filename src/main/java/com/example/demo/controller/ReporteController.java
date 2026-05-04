package com.example.demo.controller;

import com.example.demo.dto.moderacion.CrearReporteRequestDTO;
import com.example.demo.dto.moderacion.ReporteDetalleDTO;
import com.example.demo.dto.moderacion.ReporteResumenDTO;
import com.example.demo.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @PostMapping
    public ResponseEntity<ReporteDetalleDTO> crearReporte(@RequestBody CrearReporteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reporteService.crearReporte(request));
    }

    @GetMapping("/mis-reportes/{idUsuario}")
    public ResponseEntity<List<ReporteResumenDTO>> listarMisReportes(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(reporteService.listarReportesDeUsuario(idUsuario));
    }

    @GetMapping("/mis-reportes/{idUsuario}/{idReporte}")
    public ResponseEntity<ReporteDetalleDTO> obtenerDetalleMiReporte(@PathVariable Integer idUsuario,
                                                                     @PathVariable Integer idReporte) {
        return ResponseEntity.ok(reporteService.obtenerDetalleReporteDeUsuario(idUsuario, idReporte));
    }
}
