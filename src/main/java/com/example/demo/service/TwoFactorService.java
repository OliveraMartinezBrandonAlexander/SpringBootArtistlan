package com.example.demo.service;

import com.example.demo.model.TwoFactorToken;
import com.example.demo.model.Usuario;

public interface TwoFactorService {

    TwoFactorToken crearTokenLoginYEnviarCodigo(Usuario usuario);

    TwoFactorToken crearTokenActivacionYEnviarCodigo(Usuario usuario);

    TwoFactorToken validarCodigoLogin(String temporaryToken, String code);

    TwoFactorToken validarCodigoActivacion(Usuario usuario, String code);

    TwoFactorToken reenviarCodigoLogin(String temporaryToken);

    TwoFactorToken reenviarCodigoActivacion(Usuario usuario);
}
