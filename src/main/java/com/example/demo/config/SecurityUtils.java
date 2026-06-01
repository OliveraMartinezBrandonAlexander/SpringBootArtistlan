package com.example.demo.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Integer obtenerIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion no valida.");
        }

        Object principal = authentication.getPrincipal();
        if ("anonymousUser".equals(principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion no valida.");
        }

        try {
            return Integer.valueOf(principal.toString());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion no valida.");
        }
    }

    public static Integer obtenerIdUsuarioAutenticadoSiExiste() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if ("anonymousUser".equals(principal)) {
            return null;
        }

        try {
            return Integer.valueOf(principal.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Integer validarAccesoUsuario(Integer idUsuarioSolicitado) {
        if (idUsuarioSolicitado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idUsuario es obligatorio");
        }

        Integer idUsuarioAutenticado = obtenerIdUsuarioAutenticado();
        if (!idUsuarioSolicitado.equals(idUsuarioAutenticado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a este recurso.");
        }

        return idUsuarioAutenticado;
    }
}
