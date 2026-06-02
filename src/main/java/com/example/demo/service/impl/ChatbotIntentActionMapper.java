package com.example.demo.service.impl;

import com.example.demo.dto.chatbot.ChatbotActionDTO;
import com.example.demo.enums.ChatbotActionType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatbotIntentActionMapper {

    private static final int MAX_TRACKED_SESSIONS = 500;
    private static final int MAX_RECENT_INTENTS = 5;
    private static final long SESSION_TTL_MILLIS = 30 * 60 * 1000L;

    private final ConcurrentHashMap<String, IntentSessionState> sessionStates = new ConcurrentHashMap<>();

    public QuickReplySelection quickReplySelectionFor(String intent, String sessionId) {
        String safeIntent = safeIntent(intent);
        LoopState loopState = recordIntent(safeIntent, sessionId);
        if (loopState.loopDetected()) {
            return new QuickReplySelection(loopBreakerQuickReplies(), true);
        }
        return new QuickReplySelection(quickRepliesForIntent(safeIntent), false);
    }

    public List<String> quickRepliesFor(String intent) {
        return quickRepliesFor(intent, null);
    }

    public List<String> quickRepliesFor(String intent, String sessionId) {
        return quickReplySelectionFor(intent, sessionId).quickReplies();
    }

    public List<ChatbotActionDTO> actionsFor(String intent) {
        return switch (safeIntent(intent)) {
            case "ACCESO_CUENTA_PROBLEMA" -> actions(
                    action("Ir a recuperar contraseña", ChatbotActionType.NAV_RECUPERAR_CONTRASENA),
                    action("Ir a iniciar sesión", ChatbotActionType.NAV_LOGIN)
            );
            case "PUBLICAR_CONTENIDO" -> actions(
                    action("Ir a subir obra", ChatbotActionType.NAV_SUBIR_OBRA),
                    action("Ir a subir servicio", ChatbotActionType.NAV_SUBIR_SERVICIO),
                    action("Ver portafolio", ChatbotActionType.NAV_PORTAFOLIO)
            );
            case "PUBLICAR_OBRA_GUIA" -> actions(
                    action("Ir a subir obra", ChatbotActionType.NAV_SUBIR_OBRA)
            );
            case "PUBLICAR_SERVICIO_GUIA" -> actions(
                    action("Ir a subir servicio", ChatbotActionType.NAV_SUBIR_SERVICIO)
            );
            case "PORTAFOLIO_CONTENIDO_PROPIO" -> actions(
                    action("Ver portafolio", ChatbotActionType.NAV_PORTAFOLIO),
                    action("Ir a subir obra", ChatbotActionType.NAV_SUBIR_OBRA),
                    action("Ir a subir servicio", ChatbotActionType.NAV_SUBIR_SERVICIO)
            );
            case "EXPLORAR_BUSCAR_CONTENIDO" -> actions(
                    action("Ir a explorar", ChatbotActionType.NAV_EXPLORAR)
            );
            case "CONTACTO_ARTISTA" -> actions(
                    action("Ir a explorar", ChatbotActionType.NAV_EXPLORAR),
                    action("Ver mensajes", ChatbotActionType.NAV_MENSAJES)
            );
            case "COMPRA_CARRITO_ORIENTACION" -> actions(
                    action("Ver solicitudes", ChatbotActionType.NAV_SOLICITUDES),
                    action("Ver carrito", ChatbotActionType.NAV_CARRITO),
                    action("Ver transacciones", ChatbotActionType.NAV_TRANSACCIONES)
            );
            case "TRANSACCIONES_SEGUIMIENTO" -> actions(
                    action("Ver transacciones", ChatbotActionType.NAV_TRANSACCIONES)
            );
            case "SOLICITUDES_MENSAJES_ORIENTACION", "SOLICITUDES_GESTION" -> actions(
                    action("Ver solicitudes", ChatbotActionType.NAV_SOLICITUDES),
                    action("Ver notificaciones", ChatbotActionType.NAV_NOTIFICACIONES)
            );
            case "REPORTAR_CONTENIDO" -> actions(
                    action("Ir a explorar", ChatbotActionType.NAV_EXPLORAR)
            );
            case "CUENTA_SEGURIDAD_PRIVACIDAD" -> actions(
                    action("Ver perfil", ChatbotActionType.NAV_PERFIL),
                    action("Recuperar contraseña", ChatbotActionType.NAV_RECUPERAR_CONTRASENA)
            );
            case "CONVOCATORIAS_ORIENTACION" -> actions(
                    action("Ver convocatorias", ChatbotActionType.NAV_CONVOCATORIAS)
            );
            case "MODERACION_ADMIN_ORIENTACION", "ADMIN_RESTRINGIDO_ORIENTACION" -> restrictedActionsForCurrentUser();
            case "AYUDA_GENERAL_APP" -> actions(
                    action("Ir a explorar", ChatbotActionType.NAV_EXPLORAR),
                    action("Ver portafolio", ChatbotActionType.NAV_PORTAFOLIO),
                    action("Ver solicitudes", ChatbotActionType.NAV_SOLICITUDES),
                    action("Ver carrito", ChatbotActionType.NAV_CARRITO)
            );
            default -> List.of();
        };
    }

    private List<String> quickRepliesForIntent(String intent) {
        return switch (intent) {
            case "ACCESO_CUENTA_PROBLEMA" -> List.of("Recuperar contraseña", "Iniciar sesión", "Seguridad de cuenta", "Ayuda general");
            case "PUBLICAR_CONTENIDO" -> List.of("Subir obra", "Subir servicio", "Cómo comprar una obra", "Ver portafolio", "Ayuda general");
            case "PUBLICAR_OBRA_GUIA" -> List.of("Ir a subir obra", "Subir servicio", "Cómo comprar una obra", "Ver portafolio", "Ayuda general");
            case "PUBLICAR_SERVICIO_GUIA" -> List.of("Ir a subir servicio", "Subir obra", "Explorar servicios", "Ver portafolio", "Ayuda general");
            case "PORTAFOLIO_CONTENIDO_PROPIO" -> List.of("Ver portafolio", "Subir obra", "Subir servicio", "Explorar obras", "Ayuda general");
            case "EXPLORAR_BUSCAR_CONTENIDO" -> List.of("Explorar obras", "Explorar servicios", "Ver artistas", "Cómo subir una obra", "Ayuda general");
            case "CONTACTO_ARTISTA" -> List.of("Ver artistas", "Ver mensajes", "Explorar obras", "Cómo comprar una obra", "Ayuda general");
            case "COMPRA_CARRITO_ORIENTACION" -> List.of("Explorar obras", "Ver solicitudes", "Ver mis compras", "Cómo vender mi arte", "Ayuda general");
            case "TRANSACCIONES_SEGUIMIENTO" -> List.of("Ver mis compras", "Ver mis ventas", "Ver solicitudes", "Cómo comprar una obra", "Ayuda general");
            case "SOLICITUDES_MENSAJES_ORIENTACION", "SOLICITUDES_GESTION" -> List.of("Ver solicitudes", "Ver mis compras", "Cómo comprar una obra", "Ver notificaciones", "Ayuda general");
            case "FAVORITOS_ORIENTACION" -> List.of("Ver favoritos", "Explorar obras", "Ver artistas", "Cómo comprar una obra", "Ayuda general");
            case "METAS_PERSONALES_ORIENTACION" -> List.of("Ver mis metas", "Crear meta", "Ver portafolio", "Ver notificaciones", "Ayuda general");
            case "REPORTAR_CONTENIDO" -> List.of("Reportar contenido", "Explorar obras", "Seguridad de cuenta", "Ver solicitudes", "Ayuda general");
            case "CUENTA_SEGURIDAD_PRIVACIDAD" -> List.of("Ver perfil", "Recuperar contraseña", "Seguridad de cuenta", "Ver solicitudes", "Ayuda general");
            case "CONVOCATORIAS_ORIENTACION" -> List.of("Ver convocatorias", "Explorar obras", "Subir obra", "Ver portafolio", "Ayuda general");
            case "MODERACION_ADMIN_ORIENTACION", "ADMIN_RESTRINGIDO_ORIENTACION" -> restrictedQuickRepliesForCurrentUser();
            case "AYUDA_GENERAL_APP" -> List.of("Subir obra", "Subir servicio", "Explorar obras", "Cómo comprar una obra", "Seguridad de cuenta");
            case "DEFAULT_WELCOME" -> List.of("Cómo subo una obra", "Subir servicio", "Ver portafolio", "Cómo comprar una obra", "Ayuda general");
            case "DEFAULT_FALLBACK" -> List.of("Subir obra", "Subir servicio", "Explorar obras", "Recuperar contraseña", "Ayuda general");
            default -> List.of("Ayuda general", "Subir obra", "Explorar obras");
        };
    }

    private List<String> restrictedQuickRepliesForCurrentUser() {
        if (hasRole("ADMIN")) {
            return List.of("Ver moderación", "Gestionar usuarios", "Ver convocatorias", "Ver notificaciones", "Ayuda general");
        }
        if (hasRole("MODERADOR")) {
            return List.of("Ver moderación", "Reportar contenido", "Ver convocatorias", "Ver notificaciones", "Ayuda general");
        }
        return List.of("Ayuda general", "Seguridad de cuenta", "Explorar obras", "Ver portafolio", "Ver solicitudes");
    }

    private List<ChatbotActionDTO> restrictedActionsForCurrentUser() {
        if (hasRole("ADMIN")) {
            return actions(
                    action("Ver moderación", ChatbotActionType.NAV_MODERACION),
                    action("Gestionar usuarios", ChatbotActionType.NAV_GESTION_USUARIOS),
                    action("Ver convocatorias", ChatbotActionType.NAV_CONVOCATORIAS)
            );
        }
        if (hasRole("MODERADOR")) {
            return actions(
                    action("Ver moderación", ChatbotActionType.NAV_MODERACION)
            );
        }
        return List.of();
    }

    private List<String> loopBreakerQuickReplies() {
        return List.of("Cómo comprar una obra", "Subir obra", "Subir servicio", "Ver solicitudes", "Ayuda general");
    }

    private LoopState recordIntent(String intent, String sessionId) {
        String safeSessionId = safeSessionId(sessionId);
        if (safeSessionId == null) {
            return new LoopState(false);
        }

        cleanupIfNeeded();
        IntentSessionState state = sessionStates.compute(safeSessionId, (key, current) -> {
            long now = Instant.now().toEpochMilli();
            List<String> recent = current == null
                    ? new ArrayList<>()
                    : new ArrayList<>(current.recentIntents());
            recent.add(intent);
            while (recent.size() > MAX_RECENT_INTENTS) {
                recent.remove(0);
            }
            return new IntentSessionState(List.copyOf(recent), now);
        });
        return new LoopState(state != null && isLooping(state.recentIntents()));
    }

    private boolean isLooping(List<String> recentIntents) {
        if (recentIntents.size() < 2) {
            return false;
        }

        int lastIndex = recentIntents.size() - 1;
        String last = recentIntents.get(lastIndex);
        String previous = recentIntents.get(lastIndex - 1);
        if (last.equals(previous)) {
            return true;
        }

        if (recentIntents.size() >= 3) {
            String beforePrevious = recentIntents.get(lastIndex - 2);
            if (last.equals(beforePrevious) && areLoopRelated(last, previous)) {
                return true;
            }
        }

        if (recentIntents.size() >= 4) {
            String third = recentIntents.get(lastIndex - 2);
            String fourth = recentIntents.get(lastIndex - 3);
            return last.equals(third) && previous.equals(fourth) && areLoopRelated(last, previous);
        }

        return false;
    }

    private boolean areLoopRelated(String first, String second) {
        return isLoopCandidate(first) && isLoopCandidate(second);
    }

    private boolean isLoopCandidate(String intent) {
        return switch (safeIntent(intent)) {
            case "AYUDA_GENERAL_APP",
                    "EXPLORAR_BUSCAR_CONTENIDO",
                    "PUBLICAR_OBRA_GUIA",
                    "PUBLICAR_SERVICIO_GUIA",
                    "DEFAULT_FALLBACK",
                    "PORTAFOLIO_CONTENIDO_PROPIO" -> true;
            default -> false;
        };
    }

    private void cleanupIfNeeded() {
        if (sessionStates.size() <= MAX_TRACKED_SESSIONS) {
            return;
        }

        long minUpdatedAt = Instant.now().toEpochMilli() - SESSION_TTL_MILLIS;
        sessionStates.entrySet().removeIf(entry -> entry.getValue().updatedAtMillis() < minUpdatedAt);
        if (sessionStates.size() <= MAX_TRACKED_SESSIONS) {
            return;
        }

        sessionStates.keySet().stream()
                .limit(sessionStates.size() - MAX_TRACKED_SESSIONS)
                .forEach(sessionStates::remove);
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String expectedAuthority = "ROLE_" + role.toUpperCase(Locale.ROOT);
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expectedAuthority.equalsIgnoreCase(authority.getAuthority()));
    }

    private List<ChatbotActionDTO> actions(ChatbotActionDTO... actions) {
        return List.of(actions);
    }

    private ChatbotActionDTO action(String label, ChatbotActionType type) {
        return ChatbotActionDTO.builder()
                .label(label)
                .type(type.name())
                .build();
    }

    private String safeIntent(String intent) {
        return intent != null ? intent.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String safeSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }
        String safe = sessionId.trim().replaceAll("[^A-Za-z0-9_.-]", "-");
        if (safe.isEmpty()) {
            return null;
        }
        return safe.length() <= 80 ? safe : safe.substring(0, 80);
    }

    public record QuickReplySelection(List<String> quickReplies, boolean loopDetected) {
    }

    private record LoopState(boolean loopDetected) {
    }

    private record IntentSessionState(List<String> recentIntents, long updatedAtMillis) {
    }
}
