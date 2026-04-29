package com.example.demo.service.impl;

import com.example.demo.model.Usuario;
import com.example.demo.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtServiceImpl(@Value("${artistlan.jwt.secret:CHANGE_ME_ARTISTLAN_JWT_SECRET_2026_MIN_32_CHARS}") String jwtSecret,
                          @Value("${artistlan.jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generarToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(usuario.getIdUsuario()))
                .claim("usuario", usuario.getUsuario())
                .claim("correo", usuario.getCorreo())
                .claim("rol", usuario.getRol())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public Integer extraerIdUsuario(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Integer.valueOf(claims.getSubject());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
    }
}
