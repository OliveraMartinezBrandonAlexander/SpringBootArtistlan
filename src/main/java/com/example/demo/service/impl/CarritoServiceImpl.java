package com.example.demo.service.impl;

import com.example.demo.dto.CarritoDTO;
import com.example.demo.dto.CarritoRequestDTO;
import com.example.demo.model.CategoriaObras;
import com.example.demo.model.Carrito;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.CompraCarritoDetalleRepository;
import com.example.demo.repository.CompraObraRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.CarritoService;
import com.example.demo.service.FavoritosService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarritoServiceImpl implements CarritoService {

    private static final String ESTADO_EN_VENTA = "En venta";
    private static final String ESTADO_CAPTURADA = "CAPTURADA";

    private final CarritoRepository carritoRepository;
    private final CompraObraRepository compraObraRepository;
    private final CompraCarritoDetalleRepository compraCarritoDetalleRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObraRepository obraRepository;
    private final FavoritosService favoritosService;

    @Override
    @Transactional
    public CarritoDTO agregarAlCarrito(CarritoRequestDTO request) {
        validarRequest(request);

        Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + request.getIdUsuario()));

        Obra obra = obraRepository.findById(request.getIdObra())
                .orElseThrow(() -> new EntityNotFoundException("Obra no encontrada: " + request.getIdObra()));

        validarObraParaCarrito(usuario, obra);

        if (carritoRepository.existsByUsuarioIdUsuarioAndObraIdObra(request.getIdUsuario(), request.getIdObra())) {
            throw new IllegalStateException("La obra ya se encuentra en el carrito");
        }

        Carrito carrito = Carrito.builder()
                .usuario(usuario)
                .obra(obra)
                .build();

        try {
            return toDto(carritoRepository.save(carrito));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("La obra ya se encuentra en el carrito");
        }
    }

    @Override
    @Transactional
    public List<CarritoDTO> obtenerCarritoUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("idUsuario es obligatorio");
        }

        if (!usuarioRepository.existsById(idUsuario)) {
            throw new EntityNotFoundException("Usuario no encontrado: " + idUsuario);
        }

        List<Carrito> itemsCarrito = carritoRepository.findByUsuarioId(idUsuario);
        List<Carrito> itemsDisponibles = itemsCarrito.stream()
                .filter(item -> esObraComprable(item.getObra()))
                .toList();

        List<Carrito> itemsNoDisponibles = itemsCarrito.stream()
                .filter(item -> !esObraComprable(item.getObra()))
                .toList();

        if (!itemsNoDisponibles.isEmpty()) {
            carritoRepository.deleteAll(itemsNoDisponibles);
        }

        return itemsDisponibles.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void eliminarDelCarrito(CarritoRequestDTO request) {
        validarRequest(request);

        if (!usuarioRepository.existsById(request.getIdUsuario())) {
            throw new EntityNotFoundException("Usuario no encontrado: " + request.getIdUsuario());
        }

        if (!obraRepository.existsById(request.getIdObra())) {
            throw new EntityNotFoundException("Obra no encontrada: " + request.getIdObra());
        }

        int eliminados = carritoRepository.eliminarPorUsuarioYObra(
                request.getIdUsuario(),
                request.getIdObra()
        );

        if (eliminados == 0) {
            throw new EntityNotFoundException("La obra no se encuentra en el carrito del usuario");
        }
    }

    private void validarRequest(CarritoRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("El body es obligatorio");
        }
        if (request.getIdUsuario() == null) {
            throw new IllegalArgumentException("idUsuario es obligatorio");
        }
        if (request.getIdObra() == null) {
            throw new IllegalArgumentException("idObra es obligatorio");
        }
    }

    private void validarObraParaCarrito(Usuario usuario, Obra obra) {
        if (obra.getUsuario() == null || obra.getUsuario().getIdUsuario() == null) {
            throw new IllegalStateException("La obra no tiene un propietario válido");
        }

        if (Objects.equals(obra.getUsuario().getIdUsuario(), usuario.getIdUsuario())) {
            throw new IllegalArgumentException("No puedes agregar una obra propia al carrito");
        }

        String estado = obra.getEstado() != null ? obra.getEstado().trim() : "";
        if (!ESTADO_EN_VENTA.equalsIgnoreCase(estado)) {
            throw new IllegalStateException("Solo puedes agregar obras con estado En venta");
        }

        if (tieneCompraCapturada(obra.getIdObra())) {
            throw new IllegalStateException("La obra ya no está disponible para compra");
        }
    }

    private boolean esObraComprable(Obra obra) {
        if (obra == null || obra.getIdObra() == null) {
            return false;
        }

        String estado = obra.getEstado() != null ? obra.getEstado().trim() : "";
        return ESTADO_EN_VENTA.equalsIgnoreCase(estado) && !tieneCompraCapturada(obra.getIdObra());
    }

    private boolean tieneCompraCapturada(Integer idObra) {
        return compraObraRepository.existsByObraIdObraAndEstado(idObra, ESTADO_CAPTURADA)
                || compraCarritoDetalleRepository.existsByObraIdObraAndCompraCarritoEstado(idObra, ESTADO_CAPTURADA);
    }

    private CarritoDTO toDto(Carrito carrito) {
        Obra obra = carrito.getObra();
        String nombreAutor = Optional.ofNullable(obra.getUsuario())
                .map(Usuario::getUsuario)
                .orElse("Desconocido");
        String fotoPerfilAutor = Optional.ofNullable(obra.getUsuario())
                .map(Usuario::getFotoPerfil)
                .orElse(null);

        String nombreCategoria = "Sin Categoría";
        if (obra.getCategoriaObras() != null && !obra.getCategoriaObras().isEmpty()) {
            List<CategoriaObras> categorias = new ArrayList<>(obra.getCategoriaObras());
            CategoriaObras categoriaObra = categorias.get(0);
            if (categoriaObra.getCategoria() != null) {
                nombreCategoria = categoriaObra.getCategoria().getNombreCategoria();
            }
        }

        int likes = favoritosService.likesPorObra(obra.getIdObra().longValue());

        return CarritoDTO.builder()
                .idCarrito(carrito.getIdCarrito())
                .idUsuario(carrito.getUsuario().getIdUsuario())
                .idObra(obra.getIdObra())
                .idArtista(obra.getUsuario() != null ? obra.getUsuario().getIdUsuario() : null)
                .tituloObra(obra.getTitulo())
                .descripcion(obra.getDescripcion())
                .estado(obra.getEstado())
                .estadoObra(obra.getEstado())
                .precio(obra.getPrecio())
                .imagen1(obra.getImagen1())
                .imagen2(obra.getImagen2())
                .imagen3(obra.getImagen3())
                .tecnicas(obra.getTecnicas())
                .medidas(obra.getMedidas())
                .likes(likes)
                .nombreAutor(nombreAutor)
                .nombreCategoria(nombreCategoria)
                .fotoPerfilAutor(fotoPerfilAutor)
                .fechaAgregado(carrito.getFechaAgregado())
                .build();
    }
}
