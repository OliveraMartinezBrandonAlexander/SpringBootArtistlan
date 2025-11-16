package com.example.demo.controller;

import com.example.demo.dto.UsuarioIdCategoriaDTO;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.service.UsuarioIdCategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/usuarioIdCategoria")
@RequiredArgsConstructor
public class UsuarioIdCategoriaController {

    private final UsuarioIdCategoriaService service;

    // GET: Obtener todas las categorías de un usuario (formato especial)
    @GetMapping("{usuarioId}")
    public List<Map<String, Object>> obtenerCategoriasPorUsuario(@PathVariable Integer usuarioId) {
        List<UsuarioIdCategoriaDTO> categorias = service.obtenerTodasCategoriasPorUsuario(usuarioId);

        if (categorias.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> resultado = new ArrayList<>();

        // Primer objeto: el usuario
        Map<String, Object> usuarioMap = new HashMap<>();
        usuarioMap.put("idUsuario", usuarioId);
        resultado.add(usuarioMap);

        // Agregar las categorías
        for (UsuarioIdCategoriaDTO cat : categorias) {
            Map<String, Object> catMap = new HashMap<>();
            catMap.put("idCategoria", cat.getIdCategoria());
            catMap.put("nombreCategoria", cat.getNombreCategoria());
            resultado.add(catMap);
        }

        return resultado;
    }

    // GET: Listar todas las relaciones usuario-categoría
    @GetMapping
    public List<CategoriaUsuarios> listar() {
        return service.listar();
    }

    // POST: Crear una relación usuario-categoría (Publicar)
    @PostMapping
    public ResponseEntity<CategoriaUsuarios> guardar(@RequestBody CategoriaUsuarios cu) {
        CategoriaUsuarios creado = service.guardar(cu);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    // PUT: Actualizar una relación usuario-categoría
    @PutMapping("/{idUsuario}/{idCategoria}")
    public void actualizar(@PathVariable Integer idUsuario,
                           @PathVariable Integer idCategoria,
                           @RequestBody CategoriaUsuarios nuevosDatos) {
        CategoriaUsuariosID id = new CategoriaUsuariosID(idUsuario, idCategoria);
        service.actualizar(id, nuevosDatos);
    }

    // DELETE: Eliminar una relación usuario-categoría
    @DeleteMapping("/{idUsuario}/{idCategoria}")
    public void eliminar(@PathVariable Integer idUsuario,
                         @PathVariable Integer idCategoria) {
        CategoriaUsuariosID id = new CategoriaUsuariosID(idUsuario, idCategoria);
        service.eliminar(id);
    }
}