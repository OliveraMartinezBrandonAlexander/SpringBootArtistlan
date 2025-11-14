package com.example.demo.controller;

import com.example.demo.model.Categoria;
import com.example.demo.service.CategoriaService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService service;

    // GET todos
    @GetMapping
    public ResponseEntity<List<Categoria>> obtenerTodas() {
        List<Categoria> categorias = service.listar();
        if (categorias.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(categorias);
    }

    // GET por id
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtenerPorId(@PathVariable Integer id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST
    @PostMapping
    public ResponseEntity<Categoria> crear(@RequestBody Categoria categoria) {
        Categoria guardada = service.guardar(categoria);
        return ResponseEntity.ok(guardada);
    }

    // PUT por id
    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizarPorId(@PathVariable Integer id, @RequestBody Categoria nuevosDatos) {
        return service.actualizar(id, nuevosDatos)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE por id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Integer id) {
        boolean eliminado = service.eliminar(id);
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // PUT todos
    @PutMapping
    public ResponseEntity<List<Categoria>> actualizarTodas(@RequestBody List<Categoria> categorias) {
        List<Categoria> actualizadas = categorias.stream()
                .map(service::guardar)
                .toList();
        return ResponseEntity.ok(actualizadas);
    }

    // DELETE todos
    @DeleteMapping
    public ResponseEntity<Void> eliminarTodas() {
        service.listar().forEach(c -> service.eliminar(c.getIdCategoria()));
        return ResponseEntity.noContent().build();
    }
}