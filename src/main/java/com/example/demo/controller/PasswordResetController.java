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

    private static final String USER_NOT_FOUND_MESSAGE =
            "Usuario no encontrado. Verifica el nombre de usuario e int\u00E9ntalo nuevamente.";
    private static final String REQUEST_SUCCESS_MESSAGE =
            "C\u00F3digo enviado. Revisa el correo asociado a tu cuenta.";
    private static final String RESEND_SUCCESS_MESSAGE =
            "C\u00F3digo reenviado. Revisa el correo asociado a tu cuenta.";

    private final UsuarioService usuarioService;
    private final TwoFactorService twoFactorService;

    @PostMapping("/request")
    public ResponseEntity<PasswordResetResponseDTO> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO request) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorUsuario(request.getUsuarioOCorreo());
        if (usuarioOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE);
        }

        Usuario usuario = usuarioService.validarCuentaRecuperableParaPasswordReset(usuarioOpt.get());
        TwoFactorToken token = twoFactorService.crearTokenPasswordResetYEnviarCodigo(usuario);
        return ResponseEntity.ok(buildResponse(REQUEST_SUCCESS_MESSAGE, token.getTemporaryToken()));
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
                .message("Contrase\u00F1a actualizada correctamente.")
                .temporaryToken(null)
                .build());
    }

    @PostMapping("/resend")
    public ResponseEntity<PasswordResetResponseDTO> resendPasswordResetCode(
            @Valid @RequestBody PasswordResetResendRequestDTO request) {
        twoFactorService.reenviarCodigoPasswordReset(request.getTemporaryToken());
        return ResponseEntity.ok(buildResponse(RESEND_SUCCESS_MESSAGE, null));
    }

    private PasswordResetResponseDTO buildResponse(String message, String temporaryToken) {
        return PasswordResetResponseDTO.builder()
                .success(true)
                .message(message)
                .temporaryToken(temporaryToken)
                .build();
    }
}
