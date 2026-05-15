package com.example.demo.controller;

import com.example.demo.dto.ActualizarFotoPerfilRequestDTO;
import com.example.demo.dto.ArtistaResumenDTO;
import com.example.demo.dto.AuthErrorResponseDTO;
import com.example.demo.dto.CambiarRolRequestDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.UsuarioIdCategoriaDTO;
import com.example.demo.enums.EstadoModeracion;
import com.example.demo.dto.moderacion.DesactivarCuentaRequestDTO;
import com.example.demo.dto.moderacion.RespuestaModeracionDTO;
import com.example.demo.enums.EstadoCuenta;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaUsuariosRepository;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.FavoritosService;
import com.example.demo.service.JwtService;
import com.example.demo.service.TwoFactorService;
import com.example.demo.service.UsuarioIdCategoriaService;
import com.example.demo.service.UsuarioService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@AllArgsConstructor
@Validated
public class UsuarioController {

    private static final List<EstadoCuenta> ESTADOS_NO_PUBLICOS = List.of(
            EstadoCuenta.DESACTIVADO,
            EstadoCuenta.BLOQUEADO_PERMANENTE
    );
    private static final List<EstadoModeracion> ESTADOS_OBRA_NO_PUBLICOS = List.of(
            EstadoModeracion.OCULTO,
            EstadoModeracion.ELIMINADO_POR_MODERACION
    );

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final CategoriaUsuariosRepository categoriaUsuariosRepository;
    private final ObraRepository obraRepository;
    private final UsuarioIdCategoriaService usuarioIdCategoriaService;
    private final FavoritosService favoritosService;
    private final TwoFactorService twoFactorService;
    private final JwtService jwtService;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerTodos(@RequestParam(required = false) Long usuarioId) {
        List<Usuario> usuarios = usuarioRepository.findAllConCategoriasByEstadoCuentaNotIn(ESTADOS_NO_PUBLICOS);
        if (usuarios.isEmpty()) return ResponseEntity.noContent().build();

        List<UsuarioDTO> dtos = usuarios.stream()
                .map(u -> convertirADTO(u, usuarioId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/paginado")
    public ResponseEntity<PageResponseDTO<UsuarioDTO>> obtenerUsuariosPaginado(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) EstadoCuenta estadoCuenta,
            @RequestParam(required = false) Integer idCategoria,
            @PageableDefault(size = 10, sort = "idUsuario") Pageable pageable
    ) {
        String query = StringUtils.hasText(q) ? q.trim() : null;
        String rolNormalizado = StringUtils.hasText(rol) ? rol.trim() : null;

        Page<UsuarioDTO> page = usuarioRepository.findUsuariosPaginados(
                        query,
                        rolNormalizado,
                        estadoCuenta,
                        idCategoria,
                        pageable
                )
                .map(this::convertirADTOGestion);

        return ResponseEntity.ok(PageResponseDTO.fromPage(page));
    }

    @GetMapping("/artistas/paginado")
    public ResponseEntity<PageResponseDTO<ArtistaResumenDTO>> obtenerArtistasPaginado(
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Integer idCategoria,
            @PageableDefault(size = 10, sort = "idUsuario") Pageable pageable
    ) {
        String query = StringUtils.hasText(q) ? q.trim() : null;
        Integer categoriaId = idCategoria;
        String categoriaNombre = categoriaId != null
                ? null
                : (StringUtils.hasText(categoria) ? categoria.trim() : null);

        Page<Usuario> pageUsuarios = usuarioRepository.findArtistasPublicosPaginado(
                ESTADOS_NO_PUBLICOS,
                query,
                categoriaNombre,
                categoriaId,
                pageable
        );

        List<Usuario> usuariosPagina = pageUsuarios.getContent();
        Map<Integer, CategoriaUsuarios> categoriaPrincipalPorUsuario = obtenerCategoriaPrincipalPorUsuario(usuariosPagina);
        Map<Integer, List<String>> miniObrasPorUsuario = obtenerMiniObrasPorUsuario(usuariosPagina);

        Page<ArtistaResumenDTO> page = pageUsuarios.map(usuario ->
                convertirAArtistaResumenDTO(
                        usuario,
                        usuarioId,
                        categoriaPrincipalPorUsuario.get(usuario.getIdUsuario()),
                        miniObrasPorUsuario.getOrDefault(usuario.getIdUsuario(), List.of())
                )
        );

        return ResponseEntity.ok(PageResponseDTO.fromPage(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerPorId(@PathVariable Integer id,
                                                   @RequestParam(required = false) Long usuarioId) {
        return usuarioRepository.findByIdConCategorias(id)
                .map(u -> convertirADTO(u, usuarioId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/categoria")
    public ResponseEntity<UsuarioDTO> obtenerCategoriaUsuario(@PathVariable Integer id) {
        return usuarioRepository.findByIdConCategorias(id)
                .map(u -> {
                    UsuarioDTO dto = convertirADTO(u, null);
                    dto.setContrasena(null);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<List<UsuarioDTO>> crearUsuarios(HttpServletRequest request) throws IOException {
        String body = request.getReader().lines().collect(Collectors.joining());

        List<UsuarioDTO> usuarios;
        if (body.trim().startsWith("[")) {
            usuarios = mapper.readValue(body, new TypeReference<List<UsuarioDTO>>() {});
        } else {
            UsuarioDTO u = mapper.readValue(body, UsuarioDTO.class);
            usuarios = Collections.singletonList(u);
        }

        List<UsuarioDTO> creados = usuarios.stream()
                .map(usuarioService::crearUsuarioConCategoria)
                .map(u -> convertirADTO(u, null))
                .peek(dto -> dto.setContrasena(null))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(creados);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> editarUsuario(@PathVariable Integer id, @RequestBody UsuarioDTO dto) {
        Usuario actualizado = usuarioService.actualizarUsuarioConCategoria(id, dto);
        return ResponseEntity.ok(convertirADTO(actualizado, null));
    }

    @DeleteMapping
    public ResponseEntity<String> eliminarTodos() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("No se permite la eliminacion masiva de usuarios por seguridad");
    }

    @PostMapping("/{idUsuario}/desactivar-cuenta")
    public ResponseEntity<RespuestaModeracionDTO> desactivarCuenta(@PathVariable Integer idUsuario,
                                                                   @RequestBody(required = false) DesactivarCuentaRequestDTO request) {
        return ResponseEntity.ok(usuarioService.desactivarCuenta(idUsuario, request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        return autenticar(request);
    }

    @GetMapping("/existe")
    public ResponseEntity<String> existeUsuario(@RequestParam String usuario,
                                                @RequestParam String correo) {

        boolean usuarioExiste = usuarioRepository.existsByUsuario(usuario);
        boolean correoExiste = usuarioRepository.existsByCorreo(correo);

        if (usuarioExiste && correoExiste) return ResponseEntity.ok("AMBOS_DUPLICADOS");
        if (usuarioExiste) return ResponseEntity.ok("USUARIO_DUPLICADO");
        if (correoExiste) return ResponseEntity.ok("CORREO_DUPLICADO");
        return ResponseEntity.ok("OK");
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Integer id,
                                        @RequestParam Integer adminId,
                                        @Valid @RequestBody CambiarRolRequestDTO body) {
        try {
            return usuarioService.actualizarRol(id, body.getRol(), adminId)
                    .map(usuario -> {
                        UsuarioDTO dto = convertirADTO(usuario, null);
                        dto.setContrasena(null);
                        return ResponseEntity.ok(dto);
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/foto-perfil")
    public ResponseEntity<UsuarioDTO> actualizarFotoPerfil(@PathVariable int id,
                                                           @Valid @RequestBody ActualizarFotoPerfilRequestDTO body) {
        Optional<Usuario> op = usuarioRepository.findById(id);
        if (op.isEmpty()) return ResponseEntity.notFound().build();

        Usuario usuario = op.get();
        usuario.setFotoPerfil(body.getFotoPerfil());

        Usuario actualizado = usuarioRepository.save(usuario);
        return ResponseEntity.ok(convertirADTO(actualizado, null));
    }
    private UsuarioDTO convertirADTO(Usuario u, Long usuarioIdConsulta) {
        UsuarioDTO.UsuarioDTOBuilder builder = UsuarioDTO.builder()
                .idUsuario(u.getIdUsuario())
                .nombreCompleto(u.getNombreCompleto())
                .usuario(u.getUsuario())
                .contrasena(null)
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
            UsuarioIdCategoriaDTO cat = categorias.get(0); // solo la primera
            builder.idCategoria(cat.getIdCategoria());
            builder.categoria(cat.getNombreCategoria());
        }
        builder.esFavorito(esFavoritoParaUsuarioConsulta(usuarioIdConsulta, u.getIdUsuario()));
        return builder.build();
    }
    private boolean esFavoritoParaUsuarioConsulta(Long usuarioIdConsulta, Integer idArtista) {
        if (usuarioIdConsulta == null || idArtista == null) {
            return false;
        }

        if (usuarioIdConsulta > Integer.MAX_VALUE || usuarioIdConsulta < Integer.MIN_VALUE) {
            return false;
        }

        return favoritosService.esArtistaFavorito(usuarioIdConsulta.intValue(), idArtista);
    }

    private ArtistaResumenDTO convertirAArtistaResumenDTO(
            Usuario usuario,
            Integer usuarioIdConsulta,
            CategoriaUsuarios categoriaPrincipal,
            List<String> miniObras
    ) {
        Integer likes = favoritosService.likesPorArtista(usuario.getIdUsuario().longValue());
        boolean esFavorito = usuarioIdConsulta != null && favoritosService.esArtistaFavorito(usuarioIdConsulta, usuario.getIdUsuario());

        return ArtistaResumenDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombreCompleto(usuario.getNombreCompleto())
                .usuario(usuario.getUsuario())
                .descripcion(usuario.getDescripcion())
                .fotoPerfil(usuario.getFotoPerfil())
                .idCategoria(categoriaPrincipal != null && categoriaPrincipal.getCategoria() != null
                        ? categoriaPrincipal.getCategoria().getIdCategoria()
                        : null)
                .categoria(categoriaPrincipal != null && categoriaPrincipal.getCategoria() != null
                        ? categoriaPrincipal.getCategoria().getNombreCategoria()
                        : null)
                .likes(likes)
                .esFavorito(esFavorito)
                .rol(usuario.getRol())
                .miniObras(miniObras)
                .build();
    }

    private UsuarioDTO convertirADTOGestion(Usuario usuario) {
        return UsuarioDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombreCompleto(usuario.getNombreCompleto())
                .usuario(usuario.getUsuario())
                .correo(usuario.getCorreo())
                .descripcion(usuario.getDescripcion())
                .fotoPerfil(usuario.getFotoPerfil())
                .telefono(usuario.getTelefono())
                .redesSociales(usuario.getRedesSociales())
                .ubicacion(usuario.getUbicacion())
                .fechaNacimiento(usuario.getFechaNacimiento())
                .rol(usuario.getRol())
                .twoFactorEnabled(Boolean.TRUE.equals(usuario.getTwoFactorEnabled()))
                .contrasena(null)
                .build();
    }

    private Map<Integer, CategoriaUsuarios> obtenerCategoriaPrincipalPorUsuario(List<Usuario> usuarios) {
        Map<Integer, CategoriaUsuarios> categoriaPrincipal = new HashMap<>();
        if (usuarios == null || usuarios.isEmpty()) {
            return categoriaPrincipal;
        }

        List<Integer> ids = usuarios.stream()
                .map(Usuario::getIdUsuario)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return categoriaPrincipal;
        }

        List<CategoriaUsuarios> relaciones = categoriaUsuariosRepository.findByUsuarioIdsConCategoria(ids);
        for (CategoriaUsuarios cu : relaciones) {
            if (cu.getUsuario() == null || cu.getUsuario().getIdUsuario() == null) {
                continue;
            }
            Integer idUsuario = cu.getUsuario().getIdUsuario();
            if (categoriaPrincipal.containsKey(idUsuario)) {
                continue;
            }
            categoriaPrincipal.put(idUsuario, cu);
        }
        return categoriaPrincipal;
    }

    private Map<Integer, List<String>> obtenerMiniObrasPorUsuario(List<Usuario> usuarios) {
        Map<Integer, List<String>> miniObras = new HashMap<>();
        if (usuarios == null || usuarios.isEmpty()) {
            return miniObras;
        }

        List<Integer> ids = usuarios.stream()
                .map(Usuario::getIdUsuario)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return miniObras;
        }

        List<Obra> obras = obraRepository.findPublicasVisiblesPorUsuarioIds(
                ids,
                ESTADOS_OBRA_NO_PUBLICOS,
                ESTADOS_NO_PUBLICOS
        );

        Map<Integer, List<String>> candidatasPorUsuario = new HashMap<>();
        for (Obra obra : obras) {
            if (obra.getUsuario() == null || obra.getUsuario().getIdUsuario() == null) {
                continue;
            }
            Integer idUsuario = obra.getUsuario().getIdUsuario();
            if (obra.getImagen1() == null || obra.getImagen1().isBlank()) {
                continue;
            }
            candidatasPorUsuario
                    .computeIfAbsent(idUsuario, k -> new ArrayList<>())
                    .add(obra.getImagen1());
        }

        for (Integer id : ids) {
            List<String> candidatas = new ArrayList<>(candidatasPorUsuario.getOrDefault(id, new ArrayList<>()));
            if (!candidatas.isEmpty()) {
                Collections.shuffle(candidatas, ThreadLocalRandom.current());
            }

            List<String> lista = miniObras.computeIfAbsent(id, k -> new ArrayList<>());
            int limite = Math.min(3, candidatas.size());
            for (int i = 0; i < limite; i++) {
                lista.add(candidatas.get(i));
            }
            while (lista.size() < 3) {
                lista.add(null);
            }
        }

        return miniObras;
    }

    private Usuario convertirAEntidad(UsuarioDTO dto, Usuario usuarioExistente) {
        Usuario u = usuarioExistente != null ? usuarioExistente : new Usuario();
        u.setIdUsuario(dto.getIdUsuario());
        u.setNombreCompleto(dto.getNombreCompleto());
        u.setUsuario(dto.getUsuario());
        u.setCorreo(dto.getCorreo());
        u.setDescripcion(dto.getDescripcion());
        u.setFotoPerfil(dto.getFotoPerfil());
        u.setTelefono(dto.getTelefono());
        u.setRedesSociales(dto.getRedesSociales());
        u.setUbicacion(dto.getUbicacion());
        u.setFechaNacimiento(dto.getFechaNacimiento());
        u.setRol(usuarioExistente != null && usuarioExistente.getRol() != null
                ? usuarioExistente.getRol()
                : "USER");
        u.setTwoFactorEnabled(usuarioExistente != null
                ? usuarioExistente.getTwoFactorEnabled()
                : Boolean.TRUE.equals(dto.getTwoFactorEnabled()));
        if (dto.getContrasena() != null && !dto.getContrasena().isEmpty()) {
            u.setContrasena(dto.getContrasena());
        }

        return u;
    }

    private ResponseEntity<?> autenticar(LoginRequestDTO request) {
        if (request == null
                || request.getUsuarioOCorreo() == null
                || request.getUsuarioOCorreo().isBlank()
                || request.getContrasena() == null
                || request.getContrasena().isBlank()) {
            return ResponseEntity.badRequest().body("usuarioOCorreo y contrasena son obligatorios");
        }

        Optional<Usuario> user = usuarioService.buscarPorUsuarioOCorreo(request.getUsuarioOCorreo());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }

        Usuario usuarioAutenticado = user.get();
        boolean passwordValido = usuarioService.validarContrasena(
                usuarioAutenticado,
                request.getContrasena()
        );
        if (!passwordValido) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }

        try {
            usuarioAutenticado = usuarioService.validarCuentaPuedeAutenticarse(usuarioAutenticado);
        } catch (ResponseStatusException e) {
            return construirRespuestaErrorAuth(usuarioAutenticado, e);
        }

        boolean twoFactorEnabled = Boolean.TRUE.equals(usuarioAutenticado.getTwoFactorEnabled());
        if (twoFactorEnabled) {
            String temporaryToken = twoFactorService.crearTokenLoginYEnviarCodigo(usuarioAutenticado).getTemporaryToken();
            LoginResponse response = LoginResponse.builder()
                    .login(false)
                    .requires2FA(true)
                    .temporaryToken(temporaryToken)
                    .token(null)
                    .user(null)
                    .build();
            return ResponseEntity.ok(response);
        }

        UsuarioDTO dto = convertirADTO(usuarioAutenticado, null);
        dto.setContrasena(null);
        String jwt = jwtService.generarToken(usuarioAutenticado);

        LoginResponse response = LoginResponse.builder()
                .login(true)
                .requires2FA(false)
                .temporaryToken(null)
                .token(jwt)
                .user(dto)
                .build();
        response.aplicarCamposLegacyDesdeUsuario(dto);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<AuthErrorResponseDTO> construirRespuestaErrorAuth(Usuario usuario, ResponseStatusException ex) {
        String estadoCuenta = usuario != null && usuario.getEstadoCuenta() != null
                ? usuario.getEstadoCuenta().name()
                : null;
        LocalDateTime fechaFin = usuario != null ? usuario.getFechaFinSuspension() : null;

        AuthErrorResponseDTO body = AuthErrorResponseDTO.builder()
                .message(ex.getReason() != null ? ex.getReason() : "No fue posible autenticar la cuenta")
                .estadoCuenta(estadoCuenta)
                .fechaFinSuspension(fechaFin != null ? fechaFin.toString() : null)
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}
