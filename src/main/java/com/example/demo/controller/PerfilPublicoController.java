package com.example.demo.controller;

import com.example.demo.dto.publico.PerfilPublicoArtistaDTO;
import com.example.demo.service.PerfilPublicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artistas")
@RequiredArgsConstructor
public class PerfilPublicoController {

    private final PerfilPublicoService perfilPublicoService;

    @GetMapping("/{idArtista}/publico")
    public ResponseEntity<PerfilPublicoArtistaDTO> obtenerPerfilPublico(@PathVariable Integer idArtista,
                                                                         @RequestParam(required = false) Integer usuarioConsulta) {
        return ResponseEntity.ok(perfilPublicoService.obtenerPerfilPublico(idArtista, usuarioConsulta));
    }
}
