package com.example.demo.controller;

import com.example.demo.dto.TwoFactorResendRequest;
import com.example.demo.dto.TwoFactorResponse;
import com.example.demo.dto.TwoFactorVerifyActivationRequest;
import com.example.demo.dto.TwoFactorVerifyLoginRequest;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.UsuarioIdCategoriaDTO;
import com.example.demo.model.TwoFactorToken;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.FavoritosService;
import com.example.demo.service.JwtService;
import com.example.demo.service.TwoFactorService;
import com.example.demo.service.UsuarioIdCategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping({"/api/auth/2fa", "/auth/2fa"})
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioIdCategoriaService usuarioIdCategoriaService;
    private final FavoritosService favoritosService;

    @PostMapping("/verify-login")
    public ResponseEntity<TwoFactorResponse> verifyLogin(@Valid @RequestBody TwoFactorVerifyLoginRequest request) {
        TwoFactorToken token2fa = twoFactorService.validarCodigoLogin(request.getTemporaryToken(), request.getCode());

        Usuario usuario = usuarioRepository.findByIdConCategorias(token2fa.getUsuario().getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String jwt = jwtService.generarToken(usuario);
        UsuarioDTO dto = convertirADTO(usuario);
        dto.setContrasena(null);

        return ResponseEntity.ok(TwoFactorResponse.builder()
                .success(true)
                .token(jwt)
                .user(dto)
                .build());
    }

    @PostMapping("/request-activation")
    public ResponseEntity<TwoFactorResponse> requestActivation(@RequestHeader("Authorization") String authorizationHeader) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        if (Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA ya esta activado");
        }

        twoFactorService.crearTokenActivacionYEnviarCodigo(usuario);
        return ResponseEntity.ok(TwoFactorResponse.builder()
                .success(true)
                .message("Codigo enviado al correo")
                .build());
    }

    @PostMapping("/verify-activation")
    public ResponseEntity<TwoFactorResponse> verifyActivation(@RequestHeader("Authorization") String authorizationHeader,
                                                              @Valid @RequestBody TwoFactorVerifyActivationRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        if (Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA ya esta activado");
        }

        twoFactorService.validarCodigoActivacion(usuario, request.getCode());
        usuario.setTwoFactorEnabled(true);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(TwoFactorResponse.builder()
                .success(true)
                .message("2FA activado correctamente")
                .build());
    }

    @PostMapping("/resend")
    public ResponseEntity<TwoFactorResponse> resend(@RequestBody(required = false) TwoFactorResendRequest request,
                                                    @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String temporaryToken = request != null ? request.getTemporaryToken() : null;
        if (temporaryToken != null && !temporaryToken.isBlank()) {
            TwoFactorToken newToken = twoFactorService.reenviarCodigoLogin(temporaryToken);
            return ResponseEntity.ok(TwoFactorResponse.builder()
                    .success(true)
                    .message("Codigo reenviado")
                    .temporaryToken(newToken.getTemporaryToken())
                    .build());
        }

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido");
        }

        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        if (Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA ya esta activado");
        }

        twoFactorService.reenviarCodigoActivacion(usuario);
        return ResponseEntity.ok(TwoFactorResponse.builder()
                .success(true)
                .message("Codigo reenviado")
                .build());
    }

    @PostMapping("/disable")
    public ResponseEntity<TwoFactorResponse> disable(@RequestHeader("Authorization") String authorizationHeader) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        if (!Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA ya esta desactivado");
        }

        usuario.setTwoFactorEnabled(false);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(TwoFactorResponse.builder()
                .success(true)
                .message("2FA desactivado correctamente")
                .build());
    }

    private Usuario obtenerUsuarioAutenticado(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
        String jwt = authorizationHeader.substring(7);
        Integer idUsuario = jwtService.extraerIdUsuario(jwt);
        return usuarioRepository.findByIdConCategorias(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private UsuarioDTO convertirADTO(Usuario u) {
        UsuarioDTO.UsuarioDTOBuilder builder = UsuarioDTO.builder()
                .idUsuario(u.getIdUsuario())
                .nombreCompleto(u.getNombreCompleto())
                .usuario(u.getUsuario())
                .contrasena(u.getContrasena())
                .correo(u.getCorreo())
                .descripcion(u.getDescripcion())
                .fotoPerfil(u.getFotoPerfil())
                .telefono(u.getTelefono())
                .redesSociales(u.getRedesSociales())
                .ubicacion(u.getUbicacion())
                .fechaNacimiento(u.getFechaNacimiento())
                .rol(u.getRol())
                .twoFactorEnabled(Boolean.TRUE.equals(u.getTwoFactorEnabled()))
                .likes(favoritosService.likesPorArtista(u.getIdUsuario().longValue()))
                .esFavorito(false)
                .idCategoria(null)
                .categoria(null);

        List<UsuarioIdCategoriaDTO> categorias = usuarioIdCategoriaService.obtenerTodasCategoriasPorUsuario(u.getIdUsuario());
        if (!categorias.isEmpty()) {
            UsuarioIdCategoriaDTO cat = categorias.get(0);
            builder.idCategoria(cat.getIdCategoria());
            builder.categoria(cat.getNombreCategoria());
        }
        return builder.build();
    }
}
