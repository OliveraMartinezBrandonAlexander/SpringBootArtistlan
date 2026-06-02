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
    private static final double LOCAL_FALLBACK_CONFIDENCE = 0.35;
    private static final String LOOP_HINT =
            "Parece que seguimos en el mismo tema. También puedo ayudarte con otra sección de Artistlan.";

    private final ChatbotIntentActionMapper intentActionMapper;
    private final ChatbotReplyFinalizer replyFinalizer;

    @Override
    public ChatbotResponseDTO processMessage(ChatbotRequestDTO request) {
        String message = request != null ? request.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            return buildResponse(
                    "DEFAULT_FALLBACK",
                    "Escribe una pregunta sobre Artistlan y con gusto te ayudo.",
                    LOCAL_FALLBACK_CONFIDENCE,
                    request
            );
        }

        String normalized = normalize(message);
        String intent = detectIntent(normalized);
        double confidence = "DEFAULT_FALLBACK".equals(intent) ? LOCAL_FALLBACK_CONFIDENCE : LOCAL_CONFIDENCE;
        return buildResponse(intent, replyFor(intent), confidence, request);
    }

    private ChatbotResponseDTO buildResponse(String intent, String reply, Double confidence, ChatbotRequestDTO request) {
        ChatbotIntentActionMapper.QuickReplySelection quickReplySelection =
                intentActionMapper.quickReplySelectionFor(intent, request != null ? request.getSessionId() : null);
        String finalReply = quickReplySelection.loopDetected() ? reply + "\n\n" + LOOP_HINT : reply;

        return ChatbotResponseDTO.builder()
                .reply(replyFinalizer.withNaturalClosing(finalReply))
                .intent(intent)
                .source(ChatbotSource.LOCAL_FALLBACK.name())
                .confidence(confidence)
                .quickReplies(quickReplySelection.quickReplies())
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
        if (isAccessProblemIntent(text)) {
            return "ACCESO_CUENTA_PROBLEMA";
        }
        if (isAdminRestrictedIntent(text)) {
            return "ADMIN_RESTRINGIDO_ORIENTACION";
        }
        if (isPublishArtworkIntent(text)) {
            return "PUBLICAR_OBRA_GUIA";
        }
        if (isPublishServiceIntent(text)) {
            return "PUBLICAR_SERVICIO_GUIA";
        }
        if (containsAny(text, "publicar contenido", "subir contenido")) {
            return "PUBLICAR_CONTENIDO";
        }
        if (isTransactionsIntent(text)) {
            return "TRANSACCIONES_SEGUIMIENTO";
        }
        if (isPurchaseIntent(text)) {
            return "COMPRA_CARRITO_ORIENTACION";
        }
        if (isRequestsManagementIntent(text)) {
            return "SOLICITUDES_GESTION";
        }
        if (isFavoritesIntent(text)) {
            return "FAVORITOS_ORIENTACION";
        }
        if (isGoalsIntent(text)) {
            return "METAS_PERSONALES_ORIENTACION";
        }
        if (containsAny(text, "portafolio", "mis obras", "mis servicios", "mi arte", "contenido propio")) {
            return "PORTAFOLIO_CONTENIDO_PROPIO";
        }
        if (containsAny(text, "contactar", "contacto", "mensaje al artista", "hablar con artista", "vendedor")) {
            return "CONTACTO_ARTISTA";
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
        if (containsAny(text, "moderacion", "admin", "administrador", "usuarios")) {
            return "ADMIN_RESTRINGIDO_ORIENTACION";
        }
        if (containsAny(text, "ayuda", "como funciona", "que puedo hacer", "app", "artistlan")) {
            return "AYUDA_GENERAL_APP";
        }
        if (isExploreIntent(text)) {
            return "EXPLORAR_BUSCAR_CONTENIDO";
        }
        return "DEFAULT_FALLBACK";
    }

    private boolean isAccessProblemIntent(String text) {
        return containsAny(text,
                "recuperar contrasena",
                "recupero mi contrasena",
                "recupero contrasena",
                "olvide",
                "olvide mi contrasena",
                "no puedo entrar",
                "iniciar sesion",
                "login",
                "acceso",
                "cuenta bloqueada",
                "no me llega el codigo");
    }

    private boolean isPublishArtworkIntent(String text) {
        return containsAny(text,
                "subir obra",
                "subo una obra",
                "subo mi obra",
                "publicar obra",
                "publicar una obra",
                "quiero publicar una obra",
                "crear obra",
                "registrar obra",
                "cargar obra",
                "agregar una obra",
                "donde agrego una obra",
                "como subo una obra",
                "como publico una obra",
                "quiero vender mi arte",
                "como vender mi arte",
                "vender mi arte",
                "vender mi obra",
                "vender mis obras");
    }

    private boolean isPublishServiceIntent(String text) {
        return containsAny(text,
                "subir servicio",
                "subir un servicio",
                "subo un servicio",
                "subo mi servicio",
                "publicar servicio",
                "publicar un servicio",
                "crear servicio",
                "crear un servicio",
                "ofrecer servicio",
                "ofrecer un servicio",
                "quiero ofrecer un servicio",
                "registrar servicio",
                "donde creo un servicio",
                "como subo un servicio",
                "como publico un servicio");
    }

    private boolean isPurchaseIntent(String text) {
        return containsAny(text,
                "comprar",
                "como compro una obra",
                "como comprar una obra",
                "como pago una obra",
                "como uso paypal",
                "paypal",
                "carrito",
                "pago",
                "pedido",
                "solicitar compra",
                "que pasa despues de comprar",
                "proceso de compra");
    }

    private boolean isTransactionsIntent(String text) {
        return containsAny(text,
                "donde veo mis compras",
                "ver mis compras",
                "mis compras",
                "mis ventas",
                "donde veo mis ventas",
                "ver mis ventas",
                "mis transacciones",
                "transaccion",
                "transacciones",
                "compra realizada",
                "seguimiento de compra");
    }

    private boolean isRequestsManagementIntent(String text) {
        return containsAny(text,
                "como acepto una solicitud",
                "como rechazo una solicitud",
                "como cancelo una solicitud",
                "solicitudes recibidas",
                "solicitudes enviadas",
                "que pasa si acepto una solicitud",
                "aceptar solicitud",
                "rechazar solicitud",
                "cancelar solicitud");
    }

    private boolean isFavoritesIntent(String text) {
        return containsAny(text,
                "como veo favoritos",
                "ver favoritos",
                "mis favoritos",
                "guardar favorito",
                "quitar favorito",
                "obras favoritas",
                "servicios favoritos",
                "artistas favoritos");
    }

    private boolean isGoalsIntent(String text) {
        return containsAny(text,
                "que son las metas personales",
                "como creo una meta",
                "crear meta",
                "mis metas",
                "progreso de meta",
                "cancelar meta",
                "metas personales");
    }

    private boolean isAdminRestrictedIntent(String text) {
        return containsAny(text,
                "quiero ver estadisticas de la plataforma",
                "estadisticas de la plataforma",
                "gestionar usuarios",
                "editar convocatorias",
                "moderar reportes",
                "administracion",
                "panel admin");
    }

    private boolean isExploreIntent(String text) {
        return containsAny(text,
                "explorar obras",
                "buscar obras",
                "ver obras",
                "encontrar obras",
                "explorar servicios",
                "buscar servicios",
                "ver servicios",
                "encontrar servicios",
                "explorar artistas",
                "buscar artistas",
                "ver artistas",
                "encontrar artistas",
                "explorar contenido",
                "buscar contenido",
                "encontrar contenido",
                "quiero ver arte",
                "ver arte");
    }

    private String replyFor(String intent) {
        return switch (intent) {
            case "ACCESO_CUENTA_PROBLEMA" -> "Para recuperar tu contraseña, Artistlan te pide tu nombre de usuario. Si existe una cuenta recuperable, se envía un código al correo asociado; después validas ese código y eliges una nueva contraseña.";
            case "PUBLICAR_CONTENIDO" -> "Puedes publicar desde las opciones de subir obra o subir servicio. Si quieres revisar lo publicado, entra a tu portafolio.";
            case "PUBLICAR_OBRA_GUIA" -> "Para vender o publicar una obra: entra a Subir obra, completa los datos, agrega las imágenes y revisa la información antes de publicarla.";
            case "PUBLICAR_SERVICIO_GUIA" -> "Para ofrecer un servicio: entra a Subir servicio, describe lo que ofreces, agrega precio y detalles, revisa la información y publica.";
            case "PORTAFOLIO_CONTENIDO_PROPIO" -> "En tu portafolio puedes revisar tus obras y servicios publicados. También puedes ir a subir nuevo contenido.";
            case "EXPLORAR_BUSCAR_CONTENIDO" -> "Usa Explorar para buscar obras, servicios o artistas. Para ver servicios publicados, elige Explorar servicios; para publicar uno, usa Subir servicio.";
            case "CONTACTO_ARTISTA" -> "Para contactar a un artista, busca su contenido en Explorar y usa las opciones disponibles de contacto o mensajes.";
            case "COMPRA_CARRITO_ORIENTACION" -> "Para comprar una obra: explora obras disponibles, abre la obra que te interesa y envía o gestiona la solicitud de compra según el flujo de Artistlan. Cuando la solicitud se acepta, la obra puede pasar al carrito o reserva correspondiente. Finaliza el pago con PayPal cuando esté disponible y revisa el estado en Mis transacciones.";
            case "TRANSACCIONES_SEGUIMIENTO" -> "En Mis transacciones puedes revisar tus compras y ventas, junto con su estado actual y el detalle disponible.";
            case "SOLICITUDES_GESTION" -> "En Solicitudes puedes revisar recibidas y enviadas. Si eres vendedor, puedes aceptar o rechazar solicitudes pendientes; si eres comprador, puedes cancelar tus solicitudes cuando el estado lo permite.";
            case "SOLICITUDES_MENSAJES_ORIENTACION" -> "En Solicitudes y mensajes puedes revisar solicitudes recibidas, enviadas, mensajes y notificaciones relacionadas con tu actividad.";
            case "FAVORITOS_ORIENTACION" -> "En Favoritos puedes revisar obras, servicios o artistas que guardaste. También puedes explorar contenido y usar la opción de favorito cuando esté disponible.";
            case "METAS_PERSONALES_ORIENTACION" -> "Las metas personales te ayudan a dar seguimiento a objetivos como ingresos, ventas o favoritos. Puedes crear una meta, revisar su progreso y cancelarla cuando el estado lo permite.";
            case "REPORTAR_CONTENIDO" -> "Si ves contenido inadecuado, abre el contenido desde Explorar y usa la opción de reportar cuando esté disponible.";
            case "CUENTA_SEGURIDAD_PRIVACIDAD" -> "Desde tu perfil puedes revisar tus datos y seguridad. Para 2FA, puedes solicitar un código, activarlo, reenviarlo o desactivarlo si ya está activo.";
            case "CONVOCATORIAS_ORIENTACION" -> "En Convocatorias puedes revisar oportunidades y anuncios disponibles dentro de Artistlan. Crear, editar o eliminar convocatorias depende de permisos autorizados.";
            case "MODERACION_ADMIN_ORIENTACION", "ADMIN_RESTRINGIDO_ORIENTACION" -> "Las estadísticas de plataforma, gestión de usuarios, edición de convocatorias y moderación están disponibles solo para roles autorizados.";
            case "AYUDA_GENERAL_APP" -> "Puedo orientarte sobre publicar obras o servicios, explorar contenido, comprar, solicitudes, favoritos, metas, portafolio y seguridad de cuenta.";
            case "SMALLTALK_GRACIAS" -> "Con gusto. Estoy aquí para ayudarte con Artistlan.";
            case "SMALLTALK_DESPEDIDA" -> "Hasta luego. Cuando necesites ayuda con Artistlan, vuelve a escribirme.";
            case "DEFAULT_WELCOME" -> "Hola, soy el asistente de Artistlan. Puedo ayudarte con obras, servicios, compras, solicitudes, favoritos, metas, portafolio y tu cuenta.";
            default -> "No estoy seguro de haber entendido. Puedes preguntarme, por ejemplo: cómo subo una obra, cómo compro una obra o cómo recupero mi contraseña.";
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
