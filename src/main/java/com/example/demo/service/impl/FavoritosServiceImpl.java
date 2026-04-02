package com.example.demo.service.impl;

import com.example.demo.dto.FavoritosDTO;
import com.example.demo.model.Favoritos;
import com.example.demo.model.Obra;
import com.example.demo.model.Servicio;
import com.example.demo.model.Usuario;
import com.example.demo.repository.FavoritosRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.ServicioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.FavoritosService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoritosServiceImpl implements FavoritosService {

    private final FavoritosRepository favoritosRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObraRepository obraRepository;
    private final ServicioRepository servicioRepository;

    @Override
    @Transactional
    public Favoritos agregarFavorito(FavoritosDTO dto) {
        validarTargetUnico(dto);

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + dto.getIdUsuario()));

        Favoritos favorito = Favoritos.builder().usuario(usuario).build();

        if (dto.getIdObra() != null) {
            Obra obra = obraRepository.findById(dto.getIdObra())
                    .orElseThrow(() -> new EntityNotFoundException("Obra no encontrada: " + dto.getIdObra()));
            favorito.setObra(obra);
        }

        if (dto.getIdServicio() != null) {
            Servicio servicio = servicioRepository.findById(dto.getIdServicio())
                    .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado: " + dto.getIdServicio()));
            favorito.setServicio(servicio);
        }

        if (dto.getIdArtista() != null) {
            Usuario artista = usuarioRepository.findById(dto.getIdArtista())
                    .orElseThrow(() -> new EntityNotFoundException("Artista no encontrado: " + dto.getIdArtista()));
            favorito.setArtista(artista);
        }

        try {
            return favoritosRepository.save(favorito);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("El favorito ya existe para este usuario");
        }
    }

    @Override
    @Transactional
    public void eliminarFavorito(FavoritosDTO dto) {
        validarTargetUnico(dto);

        if (dto.getIdUsuario() == null) {
            throw new IllegalArgumentException("idUsuario es obligatorio");
        }

        Favoritos favorito = null;
        if (dto.getIdObra() != null) {
            favorito = favoritosRepository
                    .findByUsuarioIdUsuarioAndObraIdObra(dto.getIdUsuario(), dto.getIdObra())
                    .orElseThrow(() -> new EntityNotFoundException("Favorito de obra no encontrado"));
        } else if (dto.getIdServicio() != null) {
            favorito = favoritosRepository
                    .findByUsuarioIdUsuarioAndServicioIdServicio(dto.getIdUsuario(), dto.getIdServicio())
                    .orElseThrow(() -> new EntityNotFoundException("Favorito de servicio no encontrado"));
        } else if (dto.getIdArtista() != null) {
            favorito = favoritosRepository
                    .findByUsuarioIdUsuarioAndArtistaIdUsuario(dto.getIdUsuario(), dto.getIdArtista())
                    .orElseThrow(() -> new EntityNotFoundException("Favorito de artista no encontrado"));
        }

        favoritosRepository.delete(favorito);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favoritos> obtenerFavoritosPorUsuario(Long idUsuario) {
        return favoritosRepository.findByIdUsuario(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public int likesPorObra(Long idObra) {
        return (int) favoritosRepository.countByIdObra(idObra);
    }

    @Override
    @Transactional(readOnly = true)
    public int likesPorServicio(Long idServicio) {
        return (int) favoritosRepository.countByIdServicio(idServicio);
    }

    @Override
    @Transactional(readOnly = true)
    public int likesPorArtista(Long idArtista) {
        return (int) favoritosRepository.countByIdArtista(idArtista);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esObraFavorita(Integer idUsuario, Integer idObra) {
        if (idUsuario == null || idObra == null) return false;
        return favoritosRepository.existsByUsuarioIdUsuarioAndObraIdObra(idUsuario, idObra);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esServicioFavorito(Integer idUsuario, Integer idServicio) {
        if (idUsuario == null || idServicio == null) return false;
        return favoritosRepository.existsByUsuarioIdUsuarioAndServicioIdServicio(idUsuario, idServicio);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esArtistaFavorito(Integer idUsuario, Integer idArtista) {
        if (idUsuario == null || idArtista == null) return false;
        return favoritosRepository.existsByUsuarioIdUsuarioAndArtistaIdUsuario(idUsuario, idArtista);
    }

    private void validarTargetUnico(FavoritosDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("requerido");
        }
        if (dto.getIdUsuario() == null) {
            throw new IllegalArgumentException("idUsuario es obligatorio");
        }

        int noNulos = 0;
        if (dto.getIdObra() != null) noNulos++;
        if (dto.getIdServicio() != null) noNulos++;
        if (dto.getIdArtista() != null) noNulos++;

        if (noNulos != 1) {
            throw new IllegalArgumentException("Debe enviar exactamente uno");
        }
    }
}