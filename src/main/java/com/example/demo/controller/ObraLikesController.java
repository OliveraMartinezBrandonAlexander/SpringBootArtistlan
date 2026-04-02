package com.example.demo.controller;

import com.example.demo.service.FavoritosService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/obrasLikes")
@AllArgsConstructor
public class ObraLikesController {

    private final FavoritosService favoritosService;

    @GetMapping("/{obraId}")
    public ResponseEntity<Integer> obtenerLikesDeObra(@PathVariable Integer obraId) {
        return ResponseEntity.ok(favoritosService.likesPorObra(obraId.longValue()));
    }
}