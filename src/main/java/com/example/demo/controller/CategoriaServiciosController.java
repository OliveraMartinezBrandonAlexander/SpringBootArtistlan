package com.example.demo.controller;

import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.Servicio;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CategoriaServiciosRepository;
import com.example.demo.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categoriaServicios")
@RequiredArgsConstructor
public class CategoriaServiciosController {

    private final CategoriaServiciosRepository categoriaServiciosRepository;
    private final ServicioRepository servicioRepository;
    private final CategoriaRepository categoriaRepository;

    // GET: listar todas las relaciones
    @GetMapping
    public ResponseEntity<List<CategoriaServicios>> obtenerTodas() {
        List<CategoriaServicios> lista = categoriaServiciosRepository.findAll();
        return new ResponseEntity<>(lista, HttpStatus.OK);
    }

    // GET: obtener una relación por id
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaServicios> obtenerPorId(@PathVariable Integer id) {
        Optional<CategoriaServicios> csOpt = categoriaServiciosRepository.findById(id);
        return csOpt
                .map(cs -> new ResponseEntity<>(cs, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // POST: crear relación servicio–categoría
    @PostMapping
    public ResponseEntity<CategoriaServicios> crearRelacion(
            @RequestParam Integer idServicio,
            @RequestParam Integer idCategoria) {

        Optional<Servicio> servicioOpt = servicioRepository.findById(idServicio);
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(idCategoria);

        if (servicioOpt.isEmpty() || categoriaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        CategoriaServicios cs = CategoriaServicios.builder()
                .servicio(servicioOpt.get())
                .categoria(categoriaOpt.get())
                .build();

        CategoriaServicios guardado = categoriaServiciosRepository.save(cs);
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    // PUT: actualizar relación (cambiar servicio y/o categoría)
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaServicios> actualizarRelacion(
            @PathVariable Integer id,
            @RequestParam Integer idServicio,
            @RequestParam Integer idCategoria) {

        Optional<CategoriaServicios> csOpt = categoriaServiciosRepository.findById(id);
        if (csOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Optional<Servicio> servicioOpt = servicioRepository.findById(idServicio);
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(idCategoria);

        if (servicioOpt.isEmpty() || categoriaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        CategoriaServicios cs = csOpt.get();
        // OJO: aquí NO tocamos el id, solo actualizamos las referencias
        cs.setServicio(servicioOpt.get());
        cs.setCategoria(categoriaOpt.get());

        CategoriaServicios actualizado = categoriaServiciosRepository.save(cs);
        return new ResponseEntity<>(actualizado, HttpStatus.OK);
    }

    // DELETE: eliminar relación
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRelacion(@PathVariable Integer id) {
        if (!categoriaServiciosRepository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        categoriaServiciosRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
