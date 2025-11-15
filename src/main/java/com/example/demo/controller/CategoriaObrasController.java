package com.example.demo.controller;

import com.example.demo.dto.CategoriaObrasDto;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaObras;
import com.example.demo.model.CategoriaObrasID;
import com.example.demo.model.Obra;
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
    public ResponseEntity<CategoriaObras> crear(@RequestBody CategoriaObrasDto dto) {

        // ID compuesto
        CategoriaObrasID id = new CategoriaObrasID(
                dto.getIdObra(),
                dto.getIdCategoria()
        );

        // Referencias a Obra y Categoria (solo IDs)
        Obra obra = new Obra();

        obra.setIdObra(dto.getIdObra());


        Categoria categoria = new Categoria();
        categoria.setIdCategoria(dto.getIdCategoria());

        // Construimos la entidad
        CategoriaObras co = new CategoriaObras();
        co.setId(id);
        co.setObra(obra);
        co.setCategoria(categoria);

        CategoriaObras guardado = service.guardar(co);
        return ResponseEntity.ok(guardado);
    }


    @GetMapping
    public List<CategoriaObras> todos() {
        return service.listar();
    }


    @GetMapping("/buscar")
    public ResponseEntity<CategoriaObras> obtener(
            @RequestParam Integer idObra,
            @RequestParam Integer idCategoria) {

        CategoriaObrasID id = new CategoriaObrasID(idObra, idCategoria);
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @RequestParam Integer idObra,
            @RequestParam Integer idCategoria) {

        CategoriaObrasID id = new CategoriaObrasID(idObra, idCategoria);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
