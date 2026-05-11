package com.example.demo.config;

import com.example.demo.dto.AuthErrorResponseDTO;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.JwtService;
import com.example.demo.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            escribirError(response, HttpStatus.UNAUTHORIZED, "Token invalido");
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            Integer idUsuario = jwtService.extraerIdUsuario(token);
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
            if (usuarioOpt.isEmpty()) {
                escribirError(response, HttpStatus.UNAUTHORIZED, "Usuario no encontrado");
                return;
            }

            Usuario usuario = usuarioOpt.get();
            try {
                usuario = usuarioService.validarCuentaPuedeAutenticarse(usuario);
            } catch (ResponseStatusException ex) {
                manejarResponseStatusException(response, ex, usuario);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String rol = normalizarRol(usuario.getRol());
                String principal = String.valueOf(usuario.getIdUsuario());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ResponseStatusException ex) {
            manejarResponseStatusException(response, ex, null);
            return;
        } catch (Exception ex) {
            escribirError(response, HttpStatus.UNAUTHORIZED, "Token invalido");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void manejarResponseStatusException(HttpServletResponse response,
                                                ResponseStatusException ex,
                                                Usuario usuario) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        if (status == HttpStatus.LOCKED || status == HttpStatus.FORBIDDEN) {
            AuthErrorResponseDTO body = AuthErrorResponseDTO.builder()
                    .message(ex.getReason() != null ? ex.getReason() : "No fue posible autenticar la cuenta")
                    .estadoCuenta(usuario != null && usuario.getEstadoCuenta() != null
                            ? usuario.getEstadoCuenta().name()
                            : extraerEstadoCuentaDesdeRazon(ex.getReason()))
                    .fechaFinSuspension(usuario != null && usuario.getFechaFinSuspension() != null
                            ? usuario.getFechaFinSuspension().toString()
                            : null)
                    .build();

            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        escribirError(response, status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
    }

    private void escribirError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                java.util.Map.of(
                        "message", message
                )
        ));
    }

    private String normalizarRol(String rol) {
        if (rol == null || rol.isBlank()) {
            return "USER";
        }
        String rolNormalizado = rol.trim().toUpperCase(Locale.ROOT);
        if (rolNormalizado.startsWith("ROLE_")) {
            return rolNormalizado.substring("ROLE_".length());
        }
        return rolNormalizado;
    }

    private String extraerEstadoCuentaDesdeRazon(String razon) {
        if (razon == null) {
            return null;
        }
        String texto = razon.toUpperCase(Locale.ROOT);
        if (texto.contains("SUSPEND")) {
            return "SUSPENDIDO";
        }
        if (texto.contains("BLOQUE")) {
            return "BLOQUEADO_PERMANENTE";
        }
        if (texto.contains("DESACT")) {
            return "DESACTIVADO";
        }
        return null;
    }
}
