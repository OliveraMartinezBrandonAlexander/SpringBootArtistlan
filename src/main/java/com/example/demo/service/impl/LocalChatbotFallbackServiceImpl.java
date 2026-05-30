package com.example.demo.service.impl;

import com.example.demo.dto.chatbot.ChatbotRequestDTO;
import com.example.demo.dto.chatbot.ChatbotResponseDTO;
import com.example.demo.enums.ChatbotSource;
import com.example.demo.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LocalChatbotFallbackServiceImpl implements ChatbotService {

    private static final double LOCAL_CONFIDENCE = 1.0;

    private final ChatbotIntentActionMapper intentActionMapper;

    @Override
    public ChatbotResponseDTO processMessage(ChatbotRequestDTO request) {
        String message = request != null ? request.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            return buildResponse(
                    "DEFAULT_FALLBACK",
                    "Escribe una pregunta sobre Artistlan y con gusto te ayudo.",
                    LOCAL_CONFIDENCE
            );
        }

        String normalized = normalize(message);
        String intent = detectIntent(normalized);
        return buildResponse(intent, replyFor(intent), LOCAL_CONFIDENCE);
    }

    private ChatbotResponseDTO buildResponse(String intent, String reply, Double confidence) {
        return ChatbotResponseDTO.builder()
                .reply(reply)
                .intent(intent)
                .source(ChatbotSource.LOCAL_FALLBACK.name())
                .confidence(confidence)
                .quickReplies(intentActionMapper.quickRepliesFor(intent))
                .actions(intentActionMapper.actionsFor(intent))
                .build();
    }

    private String detectIntent(String text) {
        if (containsAny(text, "gracias", "muchas gracias", "te agradezco")) {
            return "SMALLTALK_GRACIAS";
        }
        if (containsAny(text, "adios", "hasta luego", "nos vemos", "bye")) {
            return "SMALLTALK_DESPEDIDA";
        }
        if (containsAny(text, "hola", "buen dia", "buenos dias", "buenas tardes", "buenas noches", "saludos")) {
            return "DEFAULT_WELCOME";
        }
        if (containsAny(text, "recuperar contrasena", "olvide", "no puedo entrar", "iniciar sesion", "login", "acceso", "cuenta bloqueada")) {
            return "ACCESO_CUENTA_PROBLEMA";
        }
        if (containsAny(text, "subir obra", "publicar obra", "subo una obra", "cargar obra", "crear obra")) {
            return "PUBLICAR_OBRA_GUIA";
        }
        if (containsAny(text, "subir servicio", "publicar servicio", "ofrecer servicio", "crear servicio")) {
            return "PUBLICAR_SERVICIO_GUIA";
        }
        if (containsAny(text, "publicar contenido", "publicar", "subir contenido")) {
            return "PUBLICAR_CONTENIDO";
        }
        if (containsAny(text, "portafolio", "mis obras", "mis servicios", "mi arte", "contenido propio")) {
            return "PORTAFOLIO_CONTENIDO_PROPIO";
        }
        if (containsAny(text, "contactar", "contacto", "mensaje al artista", "hablar con artista", "vendedor")) {
            return "CONTACTO_ARTISTA";
        }
        if (containsAny(text, "comprar", "carrito", "pago", "pedido", "solicitar compra")) {
            return "COMPRA_CARRITO_ORIENTACION";
        }
        if (containsAny(text, "transaccion", "transacciones", "compra realizada", "venta", "seguimiento")) {
            return "TRANSACCIONES_SEGUIMIENTO";
        }
        if (containsAny(text, "solicitudes", "mensajes", "notificaciones", "bandeja")) {
            return "SOLICITUDES_MENSAJES_ORIENTACION";
        }
        if (containsAny(text, "reportar", "reporte", "denunciar", "contenido inapropiado")) {
            return "REPORTAR_CONTENIDO";
        }
        if (containsAny(text, "seguridad", "privacidad", "perfil", "datos", "contrasena", "2fa", "doble factor")) {
            return "CUENTA_SEGURIDAD_PRIVACIDAD";
        }
        if (containsAny(text, "convocatoria", "convocatorias", "evento", "calendario")) {
            return "CONVOCATORIAS_ORIENTACION";
        }
        if (containsAny(text, "moderacion", "admin", "administrador", "gestionar usuarios", "usuarios")) {
            return "MODERACION_ADMIN_ORIENTACION";
        }
        if (containsAny(text, "ayuda", "como funciona", "que puedo hacer", "app", "artistlan")) {
            return "AYUDA_GENERAL_APP";
        }
        if (containsAny(text, "explorar", "buscar", "encontrar", "obra", "servicio", "artista", "categoria")) {
            return "EXPLORAR_BUSCAR_CONTENIDO";
        }
        return "DEFAULT_FALLBACK";
    }

    private String replyFor(String intent) {
        return switch (intent) {
            case "ACCESO_CUENTA_PROBLEMA" -> "Si tienes problemas para entrar: 1. Verifica tu usuario o correo. 2. Usa recuperar contraseña. 3. Si ya tienes cuenta, intenta iniciar sesión otra vez.";
            case "PUBLICAR_CONTENIDO" -> "Puedes publicar desde las opciones de subir obra o subir servicio. Si quieres revisar lo publicado, entra a tu portafolio.";
            case "PUBLICAR_OBRA_GUIA" -> "Para subir una obra: 1. Entra a Subir obra. 2. Completa los datos. 3. Agrega imágenes. 4. Confirma la información. 5. Publica.";
            case "PUBLICAR_SERVICIO_GUIA" -> "Para subir un servicio: 1. Entra a Subir servicio. 2. Describe lo que ofreces. 3. Agrega precio y detalles. 4. Revisa y publica.";
            case "PORTAFOLIO_CONTENIDO_PROPIO" -> "En tu portafolio puedes revisar tus obras y servicios publicados. También puedes ir a subir nuevo contenido.";
            case "EXPLORAR_BUSCAR_CONTENIDO" -> "Usa Explorar para buscar obras, servicios o artistas. Puedes revisar categorías y abrir el detalle del contenido que te interese.";
            case "CONTACTO_ARTISTA" -> "Para contactar a un artista, busca su contenido en Explorar y usa las opciones disponibles de contacto o mensajes.";
            case "COMPRA_CARRITO_ORIENTACION" -> "Para comprar: 1. Agrega una obra al carrito. 2. Revisa tu carrito. 3. Envía o confirma la solicitud. 4. Consulta el avance en solicitudes o transacciones.";
            case "TRANSACCIONES_SEGUIMIENTO" -> "En Transacciones puedes revisar tus compras y ventas, junto con su estado actual.";
            case "SOLICITUDES_MENSAJES_ORIENTACION" -> "En el centro de mensajes puedes revisar solicitudes, mensajes y notificaciones relacionadas con tu actividad.";
            case "REPORTAR_CONTENIDO" -> "Si ves contenido inadecuado, abre el contenido desde Explorar y usa la opción de reportar cuando esté disponible.";
            case "CUENTA_SEGURIDAD_PRIVACIDAD" -> "Para cuidar tu cuenta, revisa tu perfil, mantén tus datos actualizados y usa recuperar contraseña si pierdes el acceso.";
            case "CONVOCATORIAS_ORIENTACION" -> "En Convocatorias puedes revisar oportunidades y anuncios disponibles dentro de Artistlan.";
            case "MODERACION_ADMIN_ORIENTACION" -> "Las opciones de moderación y gestión de usuarios están disponibles solo para cuentas con permisos de administrador o moderador.";
            case "AYUDA_GENERAL_APP" -> "Puedo orientarte sobre publicar, explorar, comprar, solicitudes, mensajes, portafolio y seguridad de cuenta.";
            case "SMALLTALK_GRACIAS" -> "Con gusto. Estoy aquí para ayudarte con Artistlan.";
            case "SMALLTALK_DESPEDIDA" -> "Hasta luego. Cuando necesites ayuda con Artistlan, vuelve a escribirme.";
            case "DEFAULT_WELCOME" -> "Hola, soy el asistente de Artistlan. Puedo ayudarte con obras, servicios, compras, solicitudes, mensajes y tu cuenta.";
            default -> "No estoy seguro de haber entendido. Puedes preguntarme, por ejemplo: cómo subo una obra, cómo compro o cómo recupero mi contraseña.";
        };
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }
}
