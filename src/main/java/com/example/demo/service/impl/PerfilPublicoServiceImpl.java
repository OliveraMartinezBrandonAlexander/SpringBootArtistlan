package com.example.demo.service.impl;

import com.example.demo.dto.ObraDTO;
import com.example.demo.dto.ServicioDTO;
import com.example.demo.dto.publico.PerfilPublicoArtistaDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.CategoriaObras;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.Obra;
import com.example.demo.model.Servicio;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.ServicioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.FavoritosService;
import com.example.demo.service.PerfilPublicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfilPublicoServiceImpl implements PerfilPublicoService {

    private final UsuarioRepository usuarioRepository;
    private final ObraRepository obraRepository;
    private final ServicioRepository servicioRepository;
    private final FavoritosService favoritosService;

    @Override
    @Transactional(readOnly = true)
    public PerfilPublicoArtistaDTO obtenerPerfilPublico(Integer idArtista, Integer usuarioConsulta) {
        Usuario artista = usuarioRepository.findById(idArtista)
                .orElseThrow(() -> new ResourceNotFoundException("Artista no encontrado"));

        List<ObraDTO> obras = obraRepository.findByUsuarioIdUsuario(idArtista).stream()
                .map(obra -> mapObra(obra, usuarioConsulta))
                .toList();

        List<ServicioDTO> servicios = servicioRepository.findByUsuarioIdUsuario(idArtista).stream()
                .map(servicio -> mapServicio(servicio, usuarioConsulta))
                .toList();

        Set<String> categorias = obras.stream()
                .map(ObraDTO::getNombreCategoria)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return PerfilPublicoArtistaDTO.builder()
                .idUsuario(artista.getIdUsuario())
                .nombreVisible(artista.getNombreCompleto() != null ? artista.getNombreCompleto() : artista.getUsuario())
                .fotoPerfil(artista.getFotoPerfil())
                .descripcion(artista.getDescripcion())
                .ubicacion(artista.getUbicacion())
                .categorias(categorias.stream().sorted().toList())
                .obras(obras)
                .servicios(servicios)
                .build();
    }

    private ObraDTO mapObra(Obra o, Integer usuarioConsulta) {
        CategoriaObras categoria = (o.getCategoriaObras() != null && !o.getCategoriaObras().isEmpty())
                ? o.getCategoriaObras().iterator().next() : null;

        return ObraDTO.builder()
                .idObra(o.getIdObra())
                .titulo(o.getTitulo())
                .descripcion(o.getDescripcion())
                .estado(o.getEstado())
                .precio(o.getPrecio())
                .imagen1(o.getImagen1())
                .tecnicas(o.getTecnicas())
                .medidas(o.getMedidas())
                .idUsuario(o.getUsuario().getIdUsuario())
                .nombreAutor(o.getUsuario().getUsuario())
                .fotoPerfilAutor(o.getUsuario().getFotoPerfil())
                .idCategoria(categoria != null && categoria.getCategoria() != null ? categoria.getCategoria().getIdCategoria() : null)
                .nombreCategoria(categoria != null && categoria.getCategoria() != null ? categoria.getCategoria().getNombreCategoria() : null)
                .likes(favoritosService.likesPorObra(o.getIdObra().longValue()))
                .esFavorito(favoritosService.esObraFavorita(usuarioConsulta, o.getIdObra()))
                .build();
    }

    private ServicioDTO mapServicio(Servicio s, Integer usuarioConsulta) {
        CategoriaServicios categoria = (s.getCategoriasServicios() != null && !s.getCategoriasServicios().isEmpty())
                ? s.getCategoriasServicios().iterator().next() : null;

        return ServicioDTO.builder()
                .idServicio(s.getIdServicio())
                .titulo(s.getTitulo())
                .descripcion(s.getDescripcion())
                .tipoContacto(s.getTipoContacto())
                .contacto(s.getContacto())
                .tecnicas(s.getTecnicas())
                .precioMin(s.getPrecioMin())
                .precioMax(s.getPrecioMax())
                .idUsuario(s.getUsuario().getIdUsuario())
                .nombreUsuario(s.getUsuario().getUsuario())
                .fotoPerfilAutor(s.getUsuario().getFotoPerfil())
                .idCategoria(categoria != null && categoria.getCategoria() != null ? categoria.getCategoria().getIdCategoria() : null)
                .categoria(categoria != null && categoria.getCategoria() != null ? categoria.getCategoria().getNombreCategoria() : null)
                .likes(favoritosService.likesPorServicio(s.getIdServicio().longValue()))
                .esFavorito(favoritosService.esServicioFavorito(usuarioConsulta, s.getIdServicio()))
                .build();
    }
}
