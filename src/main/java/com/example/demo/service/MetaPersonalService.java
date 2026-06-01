package com.example.demo.service;

import com.example.demo.dto.meta.*;
import com.example.demo.enums.TipoMetaPersonal;

import java.util.List;
import java.util.Set;

public interface MetaPersonalService {

    List<MetaPersonalDTO> listarMisMetas();

    MetaPersonalResumenDTO obtenerResumenMisMetas();

    MetaPersonalDTO crearMeta(MetaPersonalRequestDTO request);

    MetaPersonalDTO actualizarMeta(Integer idMeta, MetaPersonalUpdateDTO request);

    MetaPersonalDTO cancelarMeta(Integer idMeta, MetaPersonalCancelRequestDTO request);

    MetaPersonalProgresoDTO obtenerProgresoMeta(Integer idMeta);

    void evaluarMetasDelUsuario(Integer idUsuario);

    void evaluarMetasDelUsuarioPorTipos(Integer idUsuario, Set<TipoMetaPersonal> tipos);
}
