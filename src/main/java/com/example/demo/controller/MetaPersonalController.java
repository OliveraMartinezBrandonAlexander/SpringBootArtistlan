package com.example.demo.controller;

import com.example.demo.dto.meta.*;
import com.example.demo.service.MetaPersonalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metas")
@RequiredArgsConstructor
public class MetaPersonalController {

    private final MetaPersonalService metaPersonalService;

    @GetMapping("/mis-metas")
    public ResponseEntity<List<MetaPersonalDTO>> listarMisMetas() {
        return ResponseEntity.ok(metaPersonalService.listarMisMetas());
    }

    @GetMapping("/resumen")
    public ResponseEntity<MetaPersonalResumenDTO> obtenerResumen() {
        return ResponseEntity.ok(metaPersonalService.obtenerResumenMisMetas());
    }

    @PostMapping
    public ResponseEntity<MetaPersonalDTO> crearMeta(@Valid @RequestBody MetaPersonalRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(metaPersonalService.crearMeta(request));
    }

    @PutMapping("/{idMeta}")
    public ResponseEntity<MetaPersonalDTO> actualizarMeta(
            @PathVariable Integer idMeta,
            @Valid @RequestBody MetaPersonalUpdateDTO request) {
        return ResponseEntity.ok(metaPersonalService.actualizarMeta(idMeta, request));
    }

    @PatchMapping("/{idMeta}/cancelar")
    public ResponseEntity<MetaPersonalDTO> cancelarMeta(
            @PathVariable Integer idMeta,
            @RequestBody(required = false) MetaPersonalCancelRequestDTO request) {
        return ResponseEntity.ok(metaPersonalService.cancelarMeta(idMeta, request));
    }

    @GetMapping("/{idMeta}/progreso")
    public ResponseEntity<MetaPersonalProgresoDTO> obtenerProgreso(@PathVariable Integer idMeta) {
        return ResponseEntity.ok(metaPersonalService.obtenerProgresoMeta(idMeta));
    }
}
