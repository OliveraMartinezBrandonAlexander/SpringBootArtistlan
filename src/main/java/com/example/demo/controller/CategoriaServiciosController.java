package com.example.demo.controller;

import com.example.demo.dto.CategoriaServiciosDto;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;
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

    // GET: obtener una relación por clave compuesta
    @GetMapping("/buscar")
    public ResponseEntity<CategoriaServicios> obtenerPorId(
            @RequestParam Integer idServicio,
            @RequestParam Integer idCategoria) {

        CategoriaServiciosID clave = new CategoriaServiciosID(idServicio, idCategoria);

        Optional<CategoriaServicios> csOpt = categoriaServiciosRepository.findById(clave);

        return csOpt
                .map(cs -> new ResponseEntity<>(cs, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // POST: crear relación
    @PostMapping
    public ResponseEntity<CategoriaServicios> crearRelacion(
            @RequestBody CategoriaServiciosDto dto) {

        Integer idServicio = dto.getIdServicio();
        Integer idCategoria = dto.getIdCategoria();

        Optional<Servicio> servicioOpt = servicioRepository.findById(idServicio);
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(idCategoria);

        if (servicioOpt.isEmpty() || categoriaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        CategoriaServiciosID clave = new CategoriaServiciosID(idServicio, idCategoria);

        if (categoriaServiciosRepository.existsById(clave)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        CategoriaServicios cs = new CategoriaServicios();
        cs.setId(clave);
        cs.setServicio(servicioOpt.get());
        cs.setCategoria(categoriaOpt.get());

        CategoriaServicios guardado = categoriaServiciosRepository.save(cs);
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    // DELETE: eliminar relación
    @DeleteMapping
    public ResponseEntity<Void> eliminarRelacion(
            @RequestParam Integer idServicio,
            @RequestParam Integer idCategoria) {

        CategoriaServiciosID clave = new CategoriaServiciosID(idServicio, idCategoria);

        if (!categoriaServiciosRepository.existsById(clave)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        categoriaServiciosRepository.deleteById(clave);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
