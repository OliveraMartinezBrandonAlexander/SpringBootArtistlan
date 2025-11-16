package com.example.demo.controller;

import com.example.demo.dto.CategoriaServiciosDto;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;
import com.example.demo.service.CategoriaServiciosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/servicios/categoria")
@RequiredArgsConstructor
public class ServiciosCategoriaController {

    private final CategoriaServiciosService service;

    // GET: Listar todas las relaciones servicio-categoría
    @GetMapping
    public List<CategoriaServicios> listar() {
        return service.listar();
    }

    // GET: Obtener una relación específica
    @GetMapping("/{idServicio}/{idCategoria}")
    public ResponseEntity<CategoriaServicios> obtenerPorId(
            @PathVariable Integer idServicio,
            @PathVariable Integer idCategoria) {
        CategoriaServiciosID id = new CategoriaServiciosID(idServicio, idCategoria);
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST: Crear una relación servicio-categoría
    @PostMapping
    public ResponseEntity<CategoriaServicios> guardar(@RequestBody CategoriaServicios cs) {
        CategoriaServicios creado = service.guardar(cs);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    // PUT: Actualizar una relación servicio-categoría
    @PutMapping("/{idServicio}/{idCategoria}")
    public ResponseEntity<CategoriaServicios> actualizar(
            @PathVariable Integer idServicio,
            @PathVariable Integer idCategoria,
            @RequestBody CategoriaServicios nuevosDatos) {

        CategoriaServiciosID id = new CategoriaServiciosID(idServicio, idCategoria);

        // Verificar si existe la relación
        Optional<CategoriaServicios> existente = service.buscarPorId(id);
        if (existente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Actualizar la relación
        nuevosDatos.setId(id);
        CategoriaServicios actualizado = service.guardar(nuevosDatos);
        return ResponseEntity.ok(actualizado);
    }

    // DELETE: Eliminar una relación servicio-categoría específica
    @DeleteMapping("/{idServicio}/{idCategoria}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer idServicio,
            @PathVariable Integer idCategoria) {

        CategoriaServiciosID id = new CategoriaServiciosID(idServicio, idCategoria);

        // Verificar si existe la relación
        Optional<CategoriaServicios> existente = service.buscarPorId(id);
        if (existente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}