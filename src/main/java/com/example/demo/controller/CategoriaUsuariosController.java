package com.example.demo.controller;

import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.service.CategoriaUsuariosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-usuarios")
public class CategoriaUsuariosController {

    @Autowired
    private CategoriaUsuariosService service;

    @PostMapping
    public ResponseEntity<CategoriaUsuarios> crear(@RequestBody CategoriaUsuarios cu) {
        return ResponseEntity.ok(service.guardar(cu));
    }

    @GetMapping
    public List<CategoriaUsuarios> todos() {
        return service.listar();
    }

    @GetMapping("/{idUsuario}/{idCategoria}")
    public ResponseEntity<CategoriaUsuarios> obtener(@PathVariable Integer idUsuario, @PathVariable Integer idCategoria) {
        CategoriaUsuariosID id = new CategoriaUsuariosID(idUsuario, idCategoria);
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{idUsuario}/{idCategoria}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer idUsuario, @PathVariable Integer idCategoria) {
        CategoriaUsuariosID id = new CategoriaUsuariosID(idUsuario, idCategoria);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}