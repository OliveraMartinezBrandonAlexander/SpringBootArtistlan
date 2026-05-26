package com.example.demo.service;

public interface EmailService {

    void enviarCodigoVerificacion(String correoDestino, String codigo);

    void enviarCodigoRecuperacionContrasena(String correoDestino, String codigo);
}
