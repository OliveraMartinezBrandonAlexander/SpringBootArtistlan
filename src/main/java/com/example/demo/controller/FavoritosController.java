package com.example.demo.controller;

import com.example.demo.dto.FavoritosDTO;
import com.example.demo.model.Favoritos;
import com.example.demo.model.Obra;
import com.example.demo.model.Servicio;
import com.example.demo.model.Usuario;
import com.example.demo.service.FavoritosService;
import com.example.demo.service.ObraService;
import com.example.demo.service.ServicioService;
import com.example.demo.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritosController {

    @Autowired
    private FavoritosService favoritosService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ObraService obraService;

    @Autowired
    private ServicioService servicioService;



    private FavoritosDTO toDto(Favoritos f) {
        return FavoritosDTO.builder()
                .id_favorito(f.getId_favorito())
                .id_usuario(f.getUsuario().getIdUsuario())
                .id_obra(f.getObra() != null ? f.getObra().getIdObra() : null)
                .id_servicio(f.getServicio() != null ? f.getServicio().getIdServicio() : null)
                .build();
    }



    @PostMapping
    public ResponseEntity<FavoritosDTO> crear(@RequestBody FavoritosDTO dto) {

        if (dto.getId_usuario() == null) {
            return ResponseEntity.badRequest().build();
        }

        // al menos uno de estos debe venir
        if (dto.getId_obra() == null && dto.getId_servicio() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Usuario debe existir
        Usuario usuario = usuarioService.buscarPorId(dto.getId_usuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Obra obra = null;
        if (dto.getId_obra() != null) {
            obra = obraService.buscarPorId(dto.getId_obra())
                    .orElseThrow(() -> new RuntimeException("Obra no encontrada"));
        }

        Servicio servicio = null;
        if (dto.getId_servicio() != null) {
            servicio = servicioService.buscarPorId(dto.getId_servicio())
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        }

        Favoritos fav = new Favoritos();
        fav.setUsuario(usuario);
        fav.setObra(obra);
        fav.setServicio(servicio);

        Favoritos guardado = favoritosService.guardar(fav);

        return ResponseEntity.ok(toDto(guardado));
    }



    @GetMapping
    public ResponseEntity<List<FavoritosDTO>> listar() {
        List<Favoritos> lista = favoritosService.listar();
        if (lista.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<FavoritosDTO> dtos = lista.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }



    @GetMapping("/{id}")
    public ResponseEntity<FavoritosDTO> obtener(@PathVariable Integer id) {
        Optional<Favoritos> opt = favoritosService.buscarPorId(id);
        return opt.map(f -> ResponseEntity.ok(toDto(f)))
                .orElse(ResponseEntity.notFound().build());
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        favoritosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
