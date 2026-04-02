package com.example.demo.controller;
import com.example.demo.dto.FavoritosDTO;
import com.example.demo.model.Favoritos;
import com.example.demo.service.FavoritosService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
public class FavoritosController {

    private final FavoritosService favoritosService;

    @PostMapping
    public ResponseEntity<?> agregarFavorito(@RequestBody FavoritosDTO dto) {
        try {
            Favoritos favorito = favoritosService.agregarFavorito(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(favorito));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }

    }
    @DeleteMapping
    public ResponseEntity<?> eliminarFavorito(@RequestBody FavoritosDTO dto) {
        try {
            favoritosService.eliminarFavorito(dto);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @GetMapping("/user/{id}")
    public ResponseEntity<List<FavoritosDTO>> favoritosPorUsuario(@PathVariable Long id) {
        List<FavoritosDTO> favoritos = favoritosService.obtenerFavoritosPorUsuario(id)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        if (favoritos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(favoritos);
    }

    @GetMapping("/likes/obra/{idObra}")
    public ResponseEntity<Integer> likesObra(@PathVariable Long idObra) {
        return ResponseEntity.ok(favoritosService.likesPorObra(idObra));
    }
    @GetMapping("/likes/servicio/{idServicio}")
    public ResponseEntity<Integer> likesServicio(@PathVariable Long idServicio) {
        return ResponseEntity.ok(favoritosService.likesPorServicio(idServicio));
    }

    @GetMapping("/likes/usuario/{idArtista}")
    public ResponseEntity<Integer> likesUsuario(@PathVariable Long idArtista) {
        return ResponseEntity.ok(favoritosService.likesPorArtista(idArtista));
    }

    private FavoritosDTO toDto(Favoritos f) {
        return FavoritosDTO.builder()
                .idFavorito(f.getIdFavorito())
                .idUsuario(f.getUsuario().getIdUsuario())
                .idObra(f.getObra() != null ? f.getObra().getIdObra() : null)
                .idServicio(f.getServicio() != null ? f.getServicio().getIdServicio() : null)
                .idArtista(f.getArtista() != null ? f.getArtista().getIdUsuario() : null)
                .build();
    }
}