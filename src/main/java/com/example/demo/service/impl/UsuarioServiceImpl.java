package com.example.demo.service.impl;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.moderacion.DesactivarCuentaRequestDTO;
import com.example.demo.dto.moderacion.RespuestaModeracionDTO;
import com.example.demo.enums.EstadoCuenta;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Categoria;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CategoriaUsuariosRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ObraService;
import com.example.demo.service.ServicioService;
import com.example.demo.service.UsuarioService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.LOCKED;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private static final Set<String> ROLES_VALIDOS = Set.of("USER", "ADMIN", "MODERADOR");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-()\\s]{8,20}$");
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    private static final int CATEGORIA_USUARIO_MIN = 19;
    private static final int CATEGORIA_USUARIO_MAX = 37;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 72;

    @Autowired
    private UsuarioRepository repo;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CategoriaUsuariosRepository categoriaUsuariosRepository;

    @Autowired
    private ObraService obraService;

    @Autowired
    private ServicioService servicioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Usuario guardarUsuario(Usuario u) {
        validarTelefono(u.getTelefono());
        if (u.getContrasena() != null && !u.getContrasena().isBlank()) {
            u.setContrasena(codificarContrasenaSiNecesaria(u.getContrasena()));
        }
        return repo.save(u);
    }

    @Override
    public Usuario crearUsuarioConCategoria(UsuarioDTO dto) {
        validarTelefono(dto.getTelefono());
        Usuario usuario = convertirAEntidad(dto);
        return repo.save(usuario);
    }

    @Override
    public List<Usuario> todosUsuarios() {
        return repo.findAllConCategorias();
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer id) {
        return repo.findByIdConCategorias(id);
    }

    @Override
    public Optional<Usuario> actualizarUsuario(Integer id, Usuario datos) {
        return repo.findById(id).map(u -> {
            u.setNombreCompleto(datos.getNombreCompleto());
            u.setUsuario(datos.getUsuario());
            u.setCorreo(datos.getCorreo());
            if (datos.getContrasena() != null && !datos.getContrasena().isBlank()) {
                u.setContrasena(codificarContrasenaSiNecesaria(datos.getContrasena()));
            }
            u.setDescripcion(datos.getDescripcion());
            u.setFotoPerfil(datos.getFotoPerfil());
            u.setTelefono(datos.getTelefono());
            u.setRedesSociales(datos.getRedesSociales());
            u.setUbicacion(datos.getUbicacion());
            u.setFechaNacimiento(datos.getFechaNacimiento());
            validarTelefono(u.getTelefono());
            return repo.save(u);
        });
    }

    @Transactional
    @Override
    public boolean eliminarUsuario(Integer id) {
        Usuario usuario = repo.findById(id).orElse(null);
        if (usuario == null) {
            return false;
        }
        desactivarUsuario(usuario, "Cuenta desactivada por solicitud del usuario");
        return true;
    }

    @Override
    public Usuario validarCuentaPuedeAutenticarse(Usuario usuario) {
        if (usuario == null) {
            throw new ResponseStatusException(FORBIDDEN, "Usuario no encontrado");
        }

        if (usuario.getEstadoCuenta() == EstadoCuenta.SUSPENDIDO) {
            LocalDateTime ahora = LocalDateTime.now();
            if (usuario.getFechaFinSuspension() != null && !usuario.getFechaFinSuspension().isAfter(ahora)) {
                usuario.setEstadoCuenta(EstadoCuenta.ACTIVO);
                usuario.setMotivoSuspension(null);
                usuario.setFechaSuspension(null);
                usuario.setFechaFinSuspension(null);
                return repo.save(usuario);
            }

            throw new ResponseStatusException(
                    LOCKED,
                    "Tu cuenta esta suspendida temporalmente. Intenta nuevamente cuando termine la suspension."
            );
        }

        if (usuario.getEstadoCuenta() == EstadoCuenta.BLOQUEADO_PERMANENTE) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Tu cuenta fue bloqueada permanentemente por moderacion."
            );
        }

        if (usuario.getEstadoCuenta() == EstadoCuenta.DESACTIVADO) {
            throw new ResponseStatusException(FORBIDDEN, "Tu cuenta esta desactivada.");
        }

        return usuario;
    }

    @Override
    public Usuario validarCuentaRecuperableParaPasswordReset(Usuario usuario) {
        if (!esCuentaRecuperableParaPasswordReset(usuario)) {
            throw new ResponseStatusException(FORBIDDEN, "La cuenta no es recuperable.");
        }

        return usuario;
    }

    @Override
    public Optional<Usuario> buscarPorUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            return Optional.empty();
        }
        return repo.findFirstByUsuarioIgnoreCase(usuario.trim());
    }

    @Override
    public Optional<Usuario> buscarCuentaRecuperablePorUsuario(String usuario) {
        return buscarPorUsuario(usuario)
                .filter(this::esCuentaRecuperableParaPasswordReset);
    }

    @Override
    public Optional<Usuario> buscarPorUsuarioOCorreo(String usuarioOCorreo) {
        if (usuarioOCorreo == null || usuarioOCorreo.isBlank()) {
            return Optional.empty();
        }
        String identificador = usuarioOCorreo.trim();
        return repo.findFirstByUsuarioIgnoreCaseOrCorreoIgnoreCase(identificador, identificador);
    }

    @Override
    public Optional<Usuario> buscarCuentaRecuperablePorUsuarioOCorreo(String usuarioOCorreo) {
        return buscarPorUsuarioOCorreo(usuarioOCorreo)
                .filter(this::esCuentaRecuperableParaPasswordReset);
    }

    @Override
    public boolean validarContrasena(Usuario usuario, String contrasenaPlana) {
        if (usuario == null || contrasenaPlana == null) {
            return false;
        }

        String contrasenaGuardada = usuario.getContrasena();
        if (contrasenaGuardada == null || contrasenaGuardada.isBlank()) {
            return false;
        }

        if (!esHashBCrypt(contrasenaGuardada)) {
            return false;
        }

        try {
            return passwordEncoder.matches(contrasenaPlana, contrasenaGuardada);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void validarNuevaContrasena(String nuevaContrasena, String confirmarContrasena) {
        if (nuevaContrasena == null || nuevaContrasena.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "nuevaContrasena es obligatoria");
        }
        if (confirmarContrasena == null || confirmarContrasena.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "confirmarContrasena es obligatoria");
        }
        if (nuevaContrasena.length() < PASSWORD_MIN_LENGTH || nuevaContrasena.length() > PASSWORD_MAX_LENGTH) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "La nueva contrasena debe tener entre " + PASSWORD_MIN_LENGTH + " y " + PASSWORD_MAX_LENGTH + " caracteres."
            );
        }
        if (confirmarContrasena.length() < PASSWORD_MIN_LENGTH || confirmarContrasena.length() > PASSWORD_MAX_LENGTH) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "confirmarContrasena debe tener entre " + PASSWORD_MIN_LENGTH + " y " + PASSWORD_MAX_LENGTH + " caracteres."
            );
        }
        if (!Objects.equals(nuevaContrasena, confirmarContrasena)) {
            throw new ResponseStatusException(BAD_REQUEST, "Las contrasenas no coinciden.");
        }
    }

    @Override
    public Usuario actualizarContrasenaPorRecuperacion(Usuario usuario, String nuevaContrasena) {
        Usuario usuarioRecuperable = validarCuentaRecuperableParaPasswordReset(usuario);
        String contrasenaActual = usuarioRecuperable.getContrasena();
        if (esHashBCrypt(contrasenaActual) && passwordEncoder.matches(nuevaContrasena, contrasenaActual)) {
            throw new ResponseStatusException(BAD_REQUEST, "La nueva contrasena debe ser diferente a la actual.");
        }

        usuarioRecuperable.setContrasena(passwordEncoder.encode(nuevaContrasena));
        return repo.save(usuarioRecuperable);
    }

    @Override
    public RespuestaModeracionDTO desactivarCuenta(Integer idUsuario, DesactivarCuentaRequestDTO request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "El request para desactivar cuenta es obligatorio");
        }
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(idUsuario);
        if (request.getIdUsuarioSolicitante() != null
                && !idUsuarioAutenticado.equals(request.getIdUsuarioSolicitante())) {
            throw new ResponseStatusException(FORBIDDEN, "No puedes desactivar una cuenta usando otro usuario solicitante.");
        }
        if (!Boolean.TRUE.equals(request.getConfirmacion())) {
            throw new ResponseStatusException(BAD_REQUEST, "confirmacion debe ser true");
        }

        Usuario solicitante = repo.findById(idUsuarioAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario solicitante no encontrado"));

        Usuario usuarioObjetivo = repo.findById(idUsuarioAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario objetivo no encontrado"));
        validarContrasenaActualSiAplica(request, solicitante, true);

        String motivo = normalizarMotivoDesactivacion(request.getMotivo());
        desactivarUsuario(usuarioObjetivo, motivo);

        return RespuestaModeracionDTO.builder()
                .success(Boolean.TRUE)
                .message("Cuenta desactivada correctamente")
                .estadoCuentaUsuario(usuarioObjetivo.getEstadoCuenta())
                .fecha(usuarioObjetivo.getFechaSuspension())
                .build();
    }

    @Override
    public Optional<Usuario> actualizarFotoPerfil(Integer id, String urlFoto) {
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(id);
        return repo.findById(idUsuarioAutenticado).map(u -> {
            u.setFotoPerfil(urlFoto);
            return repo.save(u);
        });
    }

    @Override
    public Optional<Usuario> actualizarRol(Integer id, String nuevoRol, Integer adminId) {
        Integer idAdminAutenticado = SecurityUtils.obtenerIdUsuarioAutenticado();
        if (adminId != null && !adminId.equals(idAdminAutenticado)) {
            throw new SecurityException("No puedes actuar como otro administrador");
        }

        Usuario admin = repo.findById(idAdminAutenticado)
                .orElseThrow(() -> new IllegalArgumentException("Usuario administrador no encontrado"));

        if (!"ADMIN".equalsIgnoreCase(admin.getRol())) {
            throw new SecurityException("Solo un ADMIN puede cambiar roles");
        }

        String rolNormalizado = nuevoRol == null ? "" : nuevoRol.trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rolNormalizado)) {
            throw new IllegalArgumentException("Rol invalido. Valores permitidos: USER, ADMIN, MODERADOR");
        }

        return repo.findById(id).map(usuario -> {
            usuario.setRol(rolNormalizado);
            return repo.save(usuario);
        });
    }

    @Override
    public Usuario actualizarUsuarioConCategoria(Integer id, UsuarioDTO dto) {
        Integer idUsuarioAutenticado = SecurityUtils.validarAccesoUsuario(id);
        Usuario usuario = repo.findByIdConCategorias(idUsuarioAutenticado)
                .orElseThrow(() -> new java.util.NoSuchElementException("Usuario no encontrado con ID: " + idUsuarioAutenticado));

        String usuarioNormalizado = normalizeRequired(dto.getUsuario(), "El nombre de usuario es obligatorio.");
        String correoNormalizado = normalizeRequired(dto.getCorreo(), "El correo es obligatorio.");

        if (repo.existsByUsuarioIgnoreCaseAndIdUsuarioNot(usuarioNormalizado, idUsuarioAutenticado)) {
            throw new BusinessException("El nombre de usuario ya está en uso.");
        }
        if (repo.existsByCorreoIgnoreCaseAndIdUsuarioNot(correoNormalizado, idUsuarioAutenticado)) {
            throw new BusinessException("El correo ya está en uso.");
        }

        dto.setUsuario(usuarioNormalizado);
        dto.setCorreo(correoNormalizado);

        usuario = convertirAEntidad(dto, usuario);
        validarTelefono(usuario.getTelefono());

        if (dto.getIdCategoria() != null && dto.getIdCategoria() > 0) {
            validarCategoriaUsuario(dto.getIdCategoria());
            if (usuario.getCategoriasUsuarios() == null) {
                usuario.setCategoriasUsuarios(new HashSet<>());
            }
            usuario.getCategoriasUsuarios().clear();
            categoriaUsuariosRepository.deleteByUsuarioId(idUsuarioAutenticado);

            Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                    .orElseThrow(() -> new java.util.NoSuchElementException("Categoria no encontrada con ID: " + dto.getIdCategoria()));

            CategoriaUsuarios cu = new CategoriaUsuarios();
            CategoriaUsuariosID cuId = new CategoriaUsuariosID(usuario.getIdUsuario(), categoria.getIdCategoria());

            cu.setId(cuId);
            cu.setUsuario(usuario);
            cu.setCategoria(categoria);

            categoriaUsuariosRepository.save(cu);
            usuario.getCategoriasUsuarios().add(cu);
        }

        return repo.save(usuario);
    }

    private void validarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return;
        }
        if (!PHONE_PATTERN.matcher(telefono).matches()) {
            throw new BusinessException("Telefono invalido. Debe tener entre 8 y 20 caracteres y solo numeros/simbolos telefonicos.");
        }
    }

    private void validarCategoriaUsuario(Integer idCategoria) {
        if (idCategoria < CATEGORIA_USUARIO_MIN || idCategoria > CATEGORIA_USUARIO_MAX) {
            throw new BusinessException("La categoria de usuario debe estar entre 19 y 37.");
        }
    }

    @Override
    public List<Usuario> listarAdmins() {
        return repo.findAdmins();
    }

    private void validarContrasenaActualSiAplica(DesactivarCuentaRequestDTO request,
                                                 Usuario solicitante,
                                                 boolean esMismoUsuario) {
        if (!esMismoUsuario) {
            return;
        }
        if (request.getContrasenaActual() == null || request.getContrasenaActual().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "contrasenaActual es obligatoria para desactivar tu cuenta");
        }
        if (!validarContrasena(solicitante, request.getContrasenaActual())) {
            throw new ResponseStatusException(FORBIDDEN, "La contrasena actual es incorrecta.");
        }
    }

    private void desactivarUsuario(Usuario usuario, String motivo) {
        if (usuario.getEstadoCuenta() == EstadoCuenta.DESACTIVADO) {
            throw new ResponseStatusException(CONFLICT, "La cuenta ya se encuentra desactivada");
        }
        if (usuario.getEstadoCuenta() == EstadoCuenta.BLOQUEADO_PERMANENTE) {
            throw new ResponseStatusException(CONFLICT, "No se permite la desactivacion voluntaria de una cuenta bloqueada permanentemente");
        }

        LocalDateTime ahora = LocalDateTime.now();
        usuario.setEstadoCuenta(EstadoCuenta.DESACTIVADO);
        usuario.setMotivoSuspension(motivo);
        usuario.setFechaSuspension(ahora);
        usuario.setFechaFinSuspension(null);

        // TODO: en una fase posterior bloquear la desactivacion si existen compras, ventas o procesos activos.
        obraService.eliminarPorUsuarioId(usuario.getIdUsuario());
        servicioService.ocultarPorUsuarioId(usuario.getIdUsuario());

        repo.save(usuario);
    }

    private String normalizarMotivoDesactivacion(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return "Cuenta desactivada por solicitud del usuario";
        }
        return motivo.trim();
    }

    private Usuario convertirAEntidad(UsuarioDTO dto) {
        Usuario u = new Usuario();
        u.setIdUsuario(dto.getIdUsuario());
        u.setNombreCompleto(dto.getNombreCompleto());
        u.setUsuario(dto.getUsuario());
        u.setContrasena(codificarContrasenaSiNecesaria(dto.getContrasena()));
        u.setCorreo(dto.getCorreo());
        u.setDescripcion(dto.getDescripcion());
        u.setFotoPerfil(dto.getFotoPerfil());
        u.setTelefono(dto.getTelefono());
        u.setRedesSociales(dto.getRedesSociales());
        u.setUbicacion(dto.getUbicacion());
        u.setFechaNacimiento(dto.getFechaNacimiento());
        u.setRol("USER");
        u.setTwoFactorEnabled(Boolean.TRUE.equals(dto.getTwoFactorEnabled()));
        return u;
    }

    private Usuario convertirAEntidad(UsuarioDTO dto, Usuario usuarioExistente) {

        Usuario u = usuarioExistente != null ? usuarioExistente : new Usuario();

        u.setIdUsuario(usuarioExistente != null ? usuarioExistente.getIdUsuario() : dto.getIdUsuario());

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
            u.setContrasena(codificarContrasenaSiNecesaria(dto.getContrasena()));
        }
        return u;
    }

    private boolean esHashBCrypt(String valor) {
        return valor != null && BCRYPT_PATTERN.matcher(valor).matches();
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null) {
            throw new BusinessException(errorMessage);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(errorMessage);
        }
        return normalized;
    }

    private String codificarContrasenaSiNecesaria(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            return contrasena;
        }
        if (esHashBCrypt(contrasena)) {
            return contrasena;
        }
        return passwordEncoder.encode(contrasena);
    }

    private boolean esCuentaRecuperableParaPasswordReset(Usuario usuario) {
        if (usuario == null || usuario.getEstadoCuenta() == null) {
            return false;
        }
        return usuario.getEstadoCuenta() == EstadoCuenta.ACTIVO
                || usuario.getEstadoCuenta() == EstadoCuenta.SUSPENDIDO;
    }
}
