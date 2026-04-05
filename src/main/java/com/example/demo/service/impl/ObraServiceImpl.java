package com.example.demo.service.impl;

import com.example.demo.dto.ObraDTO;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaObras;
import com.example.demo.model.CategoriaObrasID;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.CategoriaObrasRepository;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CompraCarritoDetalleRepository;
import com.example.demo.repository.CompraObraRepository;
import com.example.demo.repository.FavoritosRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ObraService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ObraServiceImpl implements ObraService {

    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaObrasRepository categoriaObraRepository;
    private final CategoriaRepository categoriaRepository;
    private final FavoritosRepository favoritosRepository;
    private final CarritoRepository carritoRepository;
    private final CompraObraRepository compraObraRepository;
    private final CompraCarritoDetalleRepository compraCarritoDetalleRepository;

    @Override
    public Obra guardar(Obra o) {
        return obraRepository.save(o);
    }

    @Override
    public List<Obra> listar() {
        return obraRepository.findAll();
    }

    @Override
    public Optional<Obra> buscarPorId(Integer id) {
        return obraRepository.findByIdConCategoria(id);
    }

    @Override
    @Transactional
    public Optional<Obra> actualizarObra(Integer id, Obra obra) {
        return obraRepository.findById(id).map(existente -> {
            existente.setTitulo(obra.getTitulo());
            existente.setDescripcion(obra.getDescripcion());
            existente.setEstado(obra.getEstado());
            existente.setPrecio(obra.getPrecio());
            existente.setImagen1(obra.getImagen1());
            existente.setImagen2(obra.getImagen2());
            existente.setImagen3(obra.getImagen3());
            existente.setTecnicas(obra.getTecnicas());
            existente.setMedidas(obra.getMedidas());
            return obraRepository.save(existente);
        });
    }

    @Override
    @Transactional
    public Obra guardarObraConCategoria(Integer usuarioId, ObraDTO obraDTO) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + usuarioId));

        Obra obra = new Obra();
        aplicarCamposEditables(obra, obraDTO);
        obra.setUsuario(usuario);

        Obra obraGuardada = obraRepository.save(obra);
        reemplazarCategoria(obraGuardada, obraDTO.getIdCategoria());

        return obraRepository.findByIdConCategoria(obraGuardada.getIdObra()).orElse(obraGuardada);
    }

    @Override
    @Transactional
    public Obra actualizarObraDeUsuario(Integer usuarioId, Integer obraId, ObraDTO obraDTO) {
        Obra obraExistente = obraRepository.findByIdConCategoria(obraId)
                .orElseThrow(() -> new NoSuchElementException("Obra no encontrada con ID: " + obraId));

        validarPertenencia(obraExistente, usuarioId);
        aplicarCamposEditables(obraExistente, obraDTO);
        reemplazarCategoria(obraExistente, obraDTO.getIdCategoria());

        Obra guardada = obraRepository.save(obraExistente);
        return obraRepository.findByIdConCategoria(guardada.getIdObra()).orElse(guardada);
    }

    @Override
    @Transactional
    public void eliminarObraDeUsuario(Integer usuarioId, Integer obraId) {
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new NoSuchElementException("Obra no encontrada con ID: " + obraId));

        validarPertenencia(obra, usuarioId);
        validarSinRelacionesParaEliminar(obraId);
        obraRepository.delete(obra);
    }

    @Override
    @Transactional
    public boolean eliminar(Integer id) {
        if (!obraRepository.existsById(id)) {
            return false;
        }

        validarSinRelacionesParaEliminar(id);
        obraRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<Obra> actualizarImagen1(Integer id, String urlImagen) {
        return obraRepository.findById(id).map(o -> {
            o.setImagen1(urlImagen);
            return obraRepository.save(o);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Obra> buscarPorUsuarioId(Integer usuarioId) {
        List<Obra> obras = obraRepository.findByUsuarioIdUsuario(usuarioId);
        for (Obra obra : obras) {
            if (obra.getCategoriaObras() != null) {
                obra.getCategoriaObras().size();
            }
        }
        return obras;
    }

    @Override
    @Transactional
    public void eliminarPorUsuarioId(Integer usuarioId) {
        obraRepository.deleteByUsuarioIdUsuario(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaDisponibleParaVenta(Integer idObra) {
        Obra obra = obraRepository.findById(idObra)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada con ID: " + idObra));

        String estado = obra.getEstado();
        return estado != null && estado.equals("En venta");
    }

    @Override
    @Transactional
    public Obra marcarComoVendida(Integer idObra) {
        Obra obra = obraRepository.findById(idObra)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada con ID: " + idObra));

        obra.setEstado("VENDIDA");
        return obraRepository.save(obra);
    }

    private void aplicarCamposEditables(Obra destino, ObraDTO origen) {
        destino.setTitulo(origen.getTitulo());
        destino.setDescripcion(origen.getDescripcion());
        destino.setEstado(origen.getEstado());
        destino.setPrecio(origen.getPrecio());
        destino.setImagen1(origen.getImagen1());
        destino.setImagen2(origen.getImagen2());
        destino.setImagen3(origen.getImagen3());
        destino.setTecnicas(origen.getTecnicas());
        destino.setMedidas(origen.getMedidas());
    }

    private void reemplazarCategoria(Obra obra, Integer idCategoria) {
        if (idCategoria == null) {
            return;
        }

        categoriaObraRepository.deleteByObraIdObra(obra.getIdObra());
        obra.getCategoriaObras().clear();

        if (idCategoria <= 0) {
            return;
        }

        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new NoSuchElementException("Categoria no encontrada con ID: " + idCategoria));

        CategoriaObras categoriaObra = new CategoriaObras(
                new CategoriaObrasID(obra.getIdObra(), categoria.getIdCategoria()),
                obra,
                categoria
        );

        obra.getCategoriaObras().add(categoriaObra);
    }

    private void validarPertenencia(Obra obra, Integer usuarioId) {
        if (obra.getUsuario() == null || !usuarioId.equals(obra.getUsuario().getIdUsuario())) {
            throw new SecurityException("La obra no pertenece al usuario indicado");
        }
    }

    private void validarSinRelacionesParaEliminar(Integer obraId) {
        if (favoritosRepository.existsByObraIdObra(obraId)
                || carritoRepository.existsByObraIdObra(obraId)
                || compraObraRepository.existsByObraIdObra(obraId)
                || compraCarritoDetalleRepository.existsByObraIdObra(obraId)) {
            throw new IllegalStateException("La obra no se puede eliminar porque tiene favoritos, carrito o compras relacionadas");
        }
    }
}
