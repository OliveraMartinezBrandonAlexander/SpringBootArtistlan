package com.example.demo.service.impl;

import com.example.demo.model.TwoFactorPurpose;
import com.example.demo.model.TwoFactorToken;
import com.example.demo.model.Usuario;
import com.example.demo.repository.TwoFactorTokenRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.TwoFactorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TwoFactorServiceImpl implements TwoFactorService {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorServiceImpl.class);
    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final int EXPIRATION_MINUTES = 5;
    private static final String TOO_MANY_ATTEMPTS_MESSAGE = "Demasiados intentos. Solicita un nuevo codigo.";

    private final TwoFactorTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public TwoFactorToken crearTokenLoginYEnviarCodigo(Usuario usuario) {
        String code = generarCodigoOtp();
        String temporaryToken = generarTemporaryTokenSeguro();
        TwoFactorToken token = crearToken(usuario, TwoFactorPurpose.LOGIN, temporaryToken, code);
        emailService.enviarCodigoVerificacion(usuario.getCorreo(), code);
        return token;
    }

    @Override
    @Transactional
    public TwoFactorToken crearTokenActivacionYEnviarCodigo(Usuario usuario) {
        String code = generarCodigoOtp();
        TwoFactorToken token = crearToken(usuario, TwoFactorPurpose.ACTIVATION, null, code);
        emailService.enviarCodigoVerificacion(usuario.getCorreo(), code);
        return token;
    }

    @Override
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TwoFactorToken validarCodigoLogin(String temporaryToken, String code) {
        TwoFactorToken token = tokenRepository.findLatestActiveByTemporaryTokenAndPurpose(temporaryToken, TwoFactorPurpose.LOGIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido"));
        return validarCodigo(token, code);
    }

    @Override
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TwoFactorToken validarCodigoActivacion(Usuario usuario, String code) {
        TwoFactorToken token = tokenRepository.findLatestActiveByUsuarioAndPurpose(usuario, TwoFactorPurpose.ACTIVATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido"));
        return validarCodigo(token, code);
    }

    @Override
    @Transactional
    public TwoFactorToken reenviarCodigoLogin(String temporaryToken) {
        TwoFactorToken currentToken = tokenRepository.findLatestByTemporaryTokenAndPurpose(temporaryToken, TwoFactorPurpose.LOGIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido"));
        if (Boolean.TRUE.equals(currentToken.getUsed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido");
        }
        return crearTokenLoginYEnviarCodigo(currentToken.getUsuario());
    }

    @Override
    @Transactional
    public TwoFactorToken reenviarCodigoActivacion(Usuario usuario) {
        return crearTokenActivacionYEnviarCodigo(usuario);
    }

    private TwoFactorToken crearToken(Usuario usuario, TwoFactorPurpose purpose, String temporaryToken, String code) {
        tokenRepository.invalidatePreviousTokens(usuario, purpose);
        LocalDateTime now = LocalDateTime.now();

        TwoFactorToken token = TwoFactorToken.builder()
                .usuario(usuario)
                .codeHash(passwordEncoder.encode(code))
                .temporaryToken(temporaryToken)
                .purpose(purpose)
                .expirationTime(now.plusMinutes(EXPIRATION_MINUTES))
                .attempts(0)
                .used(false)
                .createdAt(now)
                .build();

        return tokenRepository.save(token);
    }

    private TwoFactorToken validarCodigo(TwoFactorToken token, String code) {
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido");
        }
        if (Boolean.TRUE.equals(token.getUsed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codigo ya utilizado");
        }
        if (token.getExpirationTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codigo expirado");
        }
        if (token.getAttempts() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY_ATTEMPTS_MESSAGE);
        }

        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            Integer tokenId = token.getIdToken();
            if (tokenId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido");
            }

            int updatedRows = tokenRepository.incrementAttempts(tokenId);
            if (updatedRows == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido");
            }

            int attemptsActuales = tokenRepository.findById(tokenId)
                    .map(TwoFactorToken::getAttempts)
                    .orElse(0);

            log.warn("Intento OTP incorrecto. tokenId={}, attempts={}", tokenId, attemptsActuales);

            if (attemptsActuales >= MAX_ATTEMPTS) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY_ATTEMPTS_MESSAGE);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codigo incorrecto.");
        }

        token.setUsed(true);
        return tokenRepository.save(token);
    }

    private String generarCodigoOtp() {
        int number = secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", number);
    }

    private String generarTemporaryTokenSeguro() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
