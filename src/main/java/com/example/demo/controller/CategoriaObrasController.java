package com.example.demo.controller;

import com.example.demo.model.CategoriaObras;
import com.example.demo.model.CategoriaObrasID;
import com.example.demo.service.CategoriaObrasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-obras")
public class CategoriaObrasController {

    @Autowired
    private CategoriaObrasService service;

    @PostMapping
    public ResponseEntity<CategoriaObras> crear(@RequestBody CategoriaObras co) {
        return ResponseEntity.ok(service.guardar(co));
    }

    @GetMapping
    public List<CategoriaObras> todos() {
        return service.listar();
    }

    @GetMapping("/{idObra}/{idCategoria}")
    public ResponseEntity<CategoriaObras> obtener(@PathVariable Integer idObra, @PathVariable Integer idCategoria) {
        CategoriaObrasID id = new CategoriaObrasID(idObra, idCategoria);
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{idObra}/{idCategoria}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer idObra, @PathVariable Integer idCategoria) {
        CategoriaObrasID id = new CategoriaObrasID(idObra, idCategoria);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
