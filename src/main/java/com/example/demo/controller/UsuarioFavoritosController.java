package com.example.demo.controller;

import com.example.demo.dto.FavoritosDTO;
import com.example.demo.model.Favoritos;
import com.example.demo.service.FavoritosService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios/favoritos")
@RequiredArgsConstructor
public class UsuarioFavoritosController {

    private final FavoritosService favoritosService;

    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<FavoritosDTO>> obtenerFavoritosPorUsuario(@PathVariable Long usuarioId) {
        List<FavoritosDTO> favoritos = favoritosService.obtenerFavoritosPorUsuario(usuarioId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        if (favoritos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(favoritos);
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