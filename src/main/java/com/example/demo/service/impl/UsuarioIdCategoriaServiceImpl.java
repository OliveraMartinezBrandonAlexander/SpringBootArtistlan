package com.example.demo.service.impl;

import com.example.demo.dto.UsuarioIdCategoriaDTO;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.repository.CategoriaObrasRepository;
import com.example.demo.repository.CategoriaServiciosRepository;
import com.example.demo.repository.CategoriaUsuariosRepository;
import com.example.demo.service.UsuarioIdCategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioIdCategoriaServiceImpl implements UsuarioIdCategoriaService {

    private final CategoriaUsuariosRepository usuarioCategoriaRepo;
    private final CategoriaObrasRepository categoriaObraRepo;
    private final CategoriaServiciosRepository categoriaServicioRepo;

    // Método especial de consulta
    @Override
    public List<UsuarioIdCategoriaDTO> obtenerTodasCategoriasPorUsuario(Integer usuarioId) {
        List<UsuarioIdCategoriaDTO> resultado = new ArrayList<>();
        usuarioCategoriaRepo.findByUsuario_IdUsuario(usuarioId).forEach(uc -> {
            resultado.add(new UsuarioIdCategoriaDTO(
                    uc.getCategoria().getIdCategoria(),
                    uc.getCategoria().getNombreCategoria(),
                    "usuario"
            ));
        });


        return resultado;
    }

    // Métodos CRUD de CategoriaUsuarios
    @Override
    public CategoriaUsuarios guardar(CategoriaUsuarios cu) {
        return usuarioCategoriaRepo.save(cu);
    }

    @Override
    public List<CategoriaUsuarios> listar() {
        return usuarioCategoriaRepo.findAll();
    }

    @Override
    public Optional<CategoriaUsuarios> buscarPorId(CategoriaUsuariosID id) {
        return usuarioCategoriaRepo.findById(id);
    }

    @Override
    public void eliminar(CategoriaUsuariosID id) {
        usuarioCategoriaRepo.deleteById(id);
    }



    @Override
    public Optional<CategoriaUsuarios> actualizar(CategoriaUsuariosID id, CategoriaUsuarios nuevosDatos) {
        return usuarioCategoriaRepo.findById(id).map(cu -> {
            cu.setCategoria(nuevosDatos.getCategoria());
            cu.setUsuario(nuevosDatos.getUsuario());
            return usuarioCategoriaRepo.save(cu);
        });
    }
}
