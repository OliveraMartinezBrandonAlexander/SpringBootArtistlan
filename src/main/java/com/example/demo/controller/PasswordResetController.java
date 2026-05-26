package com.example.demo.controller;

import com.example.demo.dto.PasswordResetConfirmRequestDTO;
import com.example.demo.dto.PasswordResetRequestDTO;
import com.example.demo.dto.PasswordResetResendRequestDTO;
import com.example.demo.dto.PasswordResetResponseDTO;
import com.example.demo.model.TwoFactorToken;
import com.example.demo.model.Usuario;
import com.example.demo.service.TwoFactorService;
import com.example.demo.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping({"/api/auth/password-reset", "/auth/password-reset"})
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String GENERIC_REQUEST_MESSAGE =
            "Si la cuenta existe y es recuperable, enviaremos un codigo de recuperacion.";
    private static final String GENERIC_RESEND_MESSAGE =
            "Si la solicitud sigue siendo valida, enviaremos un nuevo codigo.";

    private final UsuarioService usuarioService;
    private final TwoFactorService twoFactorService;

    @PostMapping("/request")
    public ResponseEntity<PasswordResetResponseDTO> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO request) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarCuentaRecuperablePorUsuarioOCorreo(request.getUsuarioOCorreo());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.ok(buildResponse(GENERIC_REQUEST_MESSAGE, null));
        }

        try {
            TwoFactorToken token = twoFactorService.crearTokenPasswordResetYEnviarCodigo(usuarioOpt.get());
            return ResponseEntity.ok(buildResponse(GENERIC_REQUEST_MESSAGE, token.getTemporaryToken()));
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
                return ResponseEntity.ok(buildResponse(GENERIC_REQUEST_MESSAGE, null));
            }
            throw ex;
        }
    }

    @Transactional
    @PostMapping("/confirm")
    public ResponseEntity<PasswordResetResponseDTO> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequestDTO request) {
        usuarioService.validarNuevaContrasena(request.getNuevaContrasena(), request.getConfirmarContrasena());

        TwoFactorToken token = twoFactorService.validarCodigoPasswordReset(
                request.getTemporaryToken(),
                request.getCode()
        );

        Usuario usuario = usuarioService.validarCuentaRecuperableParaPasswordReset(token.getUsuario());
        usuarioService.actualizarContrasenaPorRecuperacion(usuario, request.getNuevaContrasena());

        return ResponseEntity.ok(PasswordResetResponseDTO.builder()
                .success(true)
                .message("Contrasena actualizada correctamente.")
                .temporaryToken(null)
                .build());
    }

    @PostMapping("/resend")
    public ResponseEntity<PasswordResetResponseDTO> resendPasswordResetCode(
            @Valid @RequestBody PasswordResetResendRequestDTO request) {
        twoFactorService.reenviarCodigoPasswordReset(request.getTemporaryToken());
        return ResponseEntity.ok(buildResponse(GENERIC_RESEND_MESSAGE, null));
    }

    private PasswordResetResponseDTO buildResponse(String message, String temporaryToken) {
        return PasswordResetResponseDTO.builder()
                .success(true)
                .message(message)
                .temporaryToken(temporaryToken)
                .build();
    }
}
