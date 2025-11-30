package com.example.demo.service.impl;

import com.example.demo.dto.ObraDTO;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaObras;
import com.example.demo.model.CategoriaObrasID;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaObrasRepository;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ObraService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ObraServiceImpl implements ObraService {

    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaObrasRepository categoriaObraRepository;
    private final CategoriaRepository categoriaRepository;

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
        return obraRepository.findById(id);
    }

    @Override
    public Optional<Obra> actualizarObra(Integer id, Obra obra) {
        return obraRepository.findById(id).map(o -> {
            o.setTitulo(obra.getTitulo());
            o.setDescripcion(obra.getDescripcion());
            o.setEstado(obra.getEstado());
            o.setPrecio(obra.getPrecio());
            o.setImagen1(obra.getImagen1());
            o.setImagen2(obra.getImagen2());
            o.setImagen3(obra.getImagen3());
            o.setTecnicas(obra.getTecnicas());
            o.setMedidas(obra.getMedidas());
            o.setLikes(obra.getLikes());
            o.setUsuario(obra.getUsuario());
            return obraRepository.save(o);
        });
    }


    @Override
    @Transactional
    public Obra guardarObraConCategoria(Integer usuarioId, ObraDTO obraDTO) {
        // 1. Obtener y verificar el usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en servicio."));

        // 2. Crear la entidad Obra y mapear campos desde el DTO
        Obra obra = new Obra();

        obra.setTitulo(obraDTO.getTitulo());
        obra.setDescripcion(obraDTO.getDescripcion());
        obra.setEstado(obraDTO.getEstado());
        obra.setPrecio(obraDTO.getPrecio());
        obra.setImagen1(obraDTO.getImagen1());
        obra.setImagen2(obraDTO.getImagen2());
        obra.setImagen3(obraDTO.getImagen3());
        obra.setTecnicas(obraDTO.getTecnicas());
        obra.setMedidas(obraDTO.getMedidas());
        obra.setLikes(obraDTO.getLikes() != null ? obraDTO.getLikes() : 0); // Asegurando que likes no sea null

        obra.setUsuario(usuario);

        Obra obraGuardada = obraRepository.save(obra);

        Integer idCategoria = obraDTO.getIdCategoria();

        if (idCategoria != null && idCategoria > 0) {

            Categoria categoria = categoriaRepository.findById(idCategoria)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + idCategoria));

            CategoriaObrasID categoriaObraID = new CategoriaObrasID(
                    obraGuardada.getIdObra(),
                    idCategoria
            );

            CategoriaObras categoriaObra = new CategoriaObras();

            categoriaObra.setId(categoriaObraID);
            categoriaObra.setObra(obraGuardada);
            categoriaObra.setCategoria(categoria);

            categoriaObraRepository.save(categoriaObra);
        }

        return obraGuardada;
    }

    @Override
    public boolean eliminar(Integer id) {
        if (obraRepository.existsById(id)) {
            obraRepository.deleteById(id);
            return true;
        }
        return false;
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
        for (Obra o : obras) {
            if (o.getCategoriaObras() != null) {
                o.getCategoriaObras().size();
            }
        }

        return obras;
    }

    @Override
    @Transactional
    public void eliminarPorUsuarioId(Integer usuarioId) {

        obraRepository.deleteByUsuarioIdUsuario(usuarioId);
    }
}