package com.example.demo.controller;

import com.example.demo.dto.ConvocatoriaDTO;
import com.example.demo.service.ConvocatoriaService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/convocatorias")
@AllArgsConstructor
public class ConvocatoriaController {

    private final ConvocatoriaService convocatoriaService;

    @GetMapping
    public ResponseEntity<List<ConvocatoriaDTO>> listarTodas() {
        List<ConvocatoriaDTO> convocatorias = convocatoriaService.listarTodas();
        return ResponseEntity.ok(convocatorias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConvocatoriaDTO> obtenerPorId(@PathVariable Integer id) {
        return convocatoriaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ConvocatoriaDTO> crear(@Valid @RequestBody ConvocatoriaDTO dto) {
        ConvocatoriaDTO creada = convocatoriaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConvocatoriaDTO> actualizar(@PathVariable Integer id,
                                                      @Valid @RequestBody ConvocatoriaDTO dto) {
        return convocatoriaService.actualizar(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        boolean eliminada = convocatoriaService.eliminar(id);
        return eliminada ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidaciones(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errores);
    }
}