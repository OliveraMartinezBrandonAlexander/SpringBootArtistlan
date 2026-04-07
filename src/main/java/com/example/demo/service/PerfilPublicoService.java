package com.example.demo.service;

import com.example.demo.dto.publico.PerfilPublicoArtistaDTO;

public interface PerfilPublicoService {
    PerfilPublicoArtistaDTO obtenerPerfilPublico(Integer idArtista, Integer usuarioConsulta);
}
