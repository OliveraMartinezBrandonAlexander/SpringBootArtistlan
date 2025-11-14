package com.example.demo.controller;

import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;
import com.example.demo.service.CategoriaServiciosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-servicios")
public class CategoriaServiciosController {

    @Autowired
    private CategoriaServiciosService service;

    @PostMapping
    public ResponseEntity<CategoriaServicios> crear(@RequestBody CategoriaServicios cs) {
        return ResponseEntity.ok(service.guardar(cs));
    }

    @GetMapping
    public List<CategoriaServicios> todos() {
        return service.listar();
    }

    @GetMapping("/{idServicio}/{idCategoria}")
    public ResponseEntity<CategoriaServicios> obtener(@PathVariable Integer idServicio, @PathVariable Integer idCategoria) {
        CategoriaServiciosID id = new CategoriaServiciosID(idServicio, idCategoria);
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{idServicio}/{idCategoria}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer idServicio, @PathVariable Integer idCategoria) {
        CategoriaServiciosID id = new CategoriaServiciosID(idServicio, idCategoria);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
