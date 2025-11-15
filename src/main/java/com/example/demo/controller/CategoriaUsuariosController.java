package com.example.demo.controller;


import com.example.demo.dto.CategoriaUsuariosDto;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.model.Usuario;
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
    public ResponseEntity<CategoriaUsuarios> crear(@RequestBody CategoriaUsuariosDto dto) {

        // ID compuesto
        CategoriaUsuariosID id = new CategoriaUsuariosID(
                dto.getIdUsuario(),
                dto.getIdCategoria()
        );

        // Referencias a Usuario y Categoria (solo ID)
        Usuario u = new Usuario();
        u.setIdUsuario(dto.getIdUsuario());

        Categoria c = new Categoria();
        c.setIdCategoria(dto.getIdCategoria());

        // Construimos la entidad
        CategoriaUsuarios cu = new CategoriaUsuarios();
        cu.setId(id);
        cu.setUsuario(u);
        cu.setCategoria(c);

        CategoriaUsuarios guardado = service.guardar(cu);
        return ResponseEntity.ok(guardado);
    }

    @GetMapping
    public List<CategoriaUsuarios> todos() {
        return service.listar();
    }

    @GetMapping("/buscar")
    public ResponseEntity<CategoriaUsuarios> obtener(
            @RequestParam Integer idUsuario,
            @RequestParam Integer idCategoria) {

        CategoriaUsuariosID id = new CategoriaUsuariosID(idUsuario, idCategoria);
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @RequestParam Integer idUsuario,
            @RequestParam Integer idCategoria) {

        CategoriaUsuariosID id = new CategoriaUsuariosID(idUsuario, idCategoria);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
