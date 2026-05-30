package com.example.demo.service.impl;

import com.example.demo.dto.chatbot.ChatbotActionDTO;
import com.example.demo.enums.ChatbotActionType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatbotIntentActionMapper {

    public List<String> quickRepliesFor(String intent) {
        return switch (safeIntent(intent)) {
            case "ACCESO_CUENTA_PROBLEMA" -> List.of("Recuperar contraseña", "Iniciar sesión", "Seguridad de cuenta");
            case "PUBLICAR_CONTENIDO" -> List.of("Subir obra", "Subir servicio", "Ver portafolio");
            case "PUBLICAR_OBRA_GUIA" -> List.of("Ir a subir obra", "Subir servicio", "Ver portafolio");
            case "PUBLICAR_SERVICIO_GUIA" -> List.of("Ir a subir servicio", "Subir obra", "Ver portafolio");
            case "PORTAFOLIO_CONTENIDO_PROPIO" -> List.of("Ver portafolio", "Subir obra", "Subir servicio");
            case "EXPLORAR_BUSCAR_CONTENIDO" -> List.of("Explorar obras", "Buscar artistas", "Ver servicios");
            case "CONTACTO_ARTISTA" -> List.of("Explorar artistas", "Ver mensajes", "Buscar contenido");
            case "COMPRA_CARRITO_ORIENTACION" -> List.of("Ver carrito", "Ver solicitudes", "Ver transacciones");
            case "TRANSACCIONES_SEGUIMIENTO" -> List.of("Mis compras", "Mis ventas", "Ver transacciones");
            case "SOLICITUDES_MENSAJES_ORIENTACION" -> List.of("Ver solicitudes", "Ver mensajes", "Notificaciones");
            case "REPORTAR_CONTENIDO" -> List.of("Explorar contenido", "Reportar obra", "Reportar servicio");
            case "CUENTA_SEGURIDAD_PRIVACIDAD" -> List.of("Ver perfil", "Recuperar contraseña", "Seguridad");
            case "CONVOCATORIAS_ORIENTACION" -> List.of("Ver convocatorias", "Explorar", "Ayuda general");
            case "MODERACION_ADMIN_ORIENTACION" -> List.of("Ver moderación", "Gestionar usuarios", "Convocatorias");
            case "AYUDA_GENERAL_APP" -> List.of("Explorar", "Portafolio", "Solicitudes", "Carrito");
            case "DEFAULT_WELCOME" -> List.of("Cómo subo una obra", "Ver portafolio", "Cómo comprar");
            case "DEFAULT_FALLBACK" -> List.of("Subir obra", "Explorar contenido", "Recuperar contraseña");
            default -> List.of();
        };
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
                    action("Ver carrito", ChatbotActionType.NAV_CARRITO),
                    action("Ver solicitudes", ChatbotActionType.NAV_SOLICITUDES),
                    action("Ver transacciones", ChatbotActionType.NAV_TRANSACCIONES)
            );
            case "TRANSACCIONES_SEGUIMIENTO" -> actions(
                    action("Ver transacciones", ChatbotActionType.NAV_TRANSACCIONES)
            );
            case "SOLICITUDES_MENSAJES_ORIENTACION" -> actions(
                    action("Ver solicitudes", ChatbotActionType.NAV_SOLICITUDES),
                    action("Ver mensajes", ChatbotActionType.NAV_MENSAJES),
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
            case "MODERACION_ADMIN_ORIENTACION" -> actions(
                    action("Ver moderación", ChatbotActionType.NAV_MODERACION),
                    action("Gestionar usuarios", ChatbotActionType.NAV_GESTION_USUARIOS)
            );
            case "AYUDA_GENERAL_APP" -> actions(
                    action("Ir a explorar", ChatbotActionType.NAV_EXPLORAR),
                    action("Ver portafolio", ChatbotActionType.NAV_PORTAFOLIO),
                    action("Ver solicitudes", ChatbotActionType.NAV_SOLICITUDES),
                    action("Ver carrito", ChatbotActionType.NAV_CARRITO)
            );
            default -> List.of();
        };
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
        return intent != null ? intent.trim().toUpperCase() : "";
    }
}
