package com.example.demo.controller;

import com.example.demo.dto.admin.*;
import com.example.demo.enums.AdminDashboardTipo;
import com.example.demo.enums.AdminTipoDatoEstadistica;
import com.example.demo.enums.AdminTipoEstadistica;
import com.example.demo.service.AdminEstadisticasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/estadisticas")
@RequiredArgsConstructor
public class AdminEstadisticasController {

    private final AdminEstadisticasService adminEstadisticasService;

    @GetMapping("/categorias")
    public ResponseEntity<List<AdminCategoriaStatsDTO>> obtenerCantidadPorCategoria(
            @RequestParam AdminDashboardTipo tipo) {
        return ResponseEntity.ok(adminEstadisticasService.obtenerCantidadPorCategoria(tipo));
    }

    @GetMapping("/ventas-semanales")
    public ResponseEntity<AdminSerieTemporalDTO> obtenerVentasSemanales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(adminEstadisticasService.obtenerVentasSemanales(fecha));
    }

    @GetMapping("/ranking")
    public ResponseEntity<AdminRankingResponseDTO> obtenerRanking(
            @RequestParam AdminDashboardTipo tipo,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(adminEstadisticasService.obtenerRanking(tipo, limit));
    }

    @GetMapping("/crecimiento")
    public ResponseEntity<AdminCrecimientoDTO> obtenerCrecimiento(
            @RequestParam AdminDashboardTipo tipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(adminEstadisticasService.obtenerCrecimientoSemanal(tipo, fecha));
    }

    @GetMapping("/observaciones")
    public ResponseEntity<List<AdminObservacionDTO>> listarObservaciones(
            @RequestParam(required = false) AdminTipoEstadistica tipoEstadistica,
            @RequestParam(required = false) AdminTipoDatoEstadistica tipoDato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicioPeriodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFinPeriodo) {
        return ResponseEntity.ok(adminEstadisticasService.listarObservaciones(
                tipoEstadistica,
                tipoDato,
                fechaInicioPeriodo,
                fechaFinPeriodo
        ));
    }

    @PostMapping("/observaciones")
    public ResponseEntity<AdminObservacionDTO> crearObservacion(
            @Valid @RequestBody AdminObservacionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminEstadisticasService.crearObservacion(request));
    }

    @PutMapping("/observaciones/{id}")
    public ResponseEntity<AdminObservacionDTO> actualizarObservacion(
            @PathVariable Integer id,
            @Valid @RequestBody AdminObservacionRequestDTO request) {
        return ResponseEntity.ok(adminEstadisticasService.actualizarObservacion(id, request));
    }

    @DeleteMapping("/observaciones/{id}")
    public ResponseEntity<Void> eliminarObservacion(@PathVariable Integer id) {
        adminEstadisticasService.eliminarObservacion(id);
        return ResponseEntity.noContent().build();
    }
}
