package com.example.demo.service;

import com.example.demo.model.Usuario;

public interface JwtService {

    String generarToken(Usuario usuario);

    Integer extraerIdUsuario(String token);
}
