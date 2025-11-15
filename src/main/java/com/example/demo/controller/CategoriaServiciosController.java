package com.example.demo.controller;

import com.example.demo.dto.CategoriaServiciosDto;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;
import com.example.demo.model.Servicio;
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
    public ResponseEntity<CategoriaServicios> crear(@RequestBody CategoriaServiciosDto dto) {

        // ID compuesto
        CategoriaServiciosID id = new CategoriaServiciosID(
                dto.getIdServicio(),
                dto.getIdCategoria()
        );

        // Referencias a Servicio y Categoria (solo IDs)
        Servicio s = new Servicio();

        s.setIdServicio(dto.getIdServicio());


        Categoria c = new Categoria();
        c.setIdCategoria(dto.getIdCategoria());

        // Construimos la entidad
        CategoriaServicios cs = new CategoriaServicios();
        cs.setId(id);
        cs.setServicio(s);
        cs.setCategoria(c);

        CategoriaServicios guardado = service.guardar(cs);
        return ResponseEntity.ok(guardado);
    }


    @GetMapping
    public List<CategoriaServicios> todos() {
        return service.listar();
    }


    @GetMapping("/buscar")
    public ResponseEntity<CategoriaServicios> obtener(
            @RequestParam Integer idServicio,
            @RequestParam Integer idCategoria) {

        CategoriaServiciosID id = new CategoriaServiciosID(idServicio, idCategoria);
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @RequestParam Integer idServicio,
            @RequestParam Integer idCategoria) {

        CategoriaServiciosID id = new CategoriaServiciosID(idServicio, idCategoria);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
