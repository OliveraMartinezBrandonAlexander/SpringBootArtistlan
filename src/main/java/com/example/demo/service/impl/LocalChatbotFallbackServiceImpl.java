package com.example.demo.service.impl;

import com.example.demo.dto.chatbot.ChatbotRequestDTO;
import com.example.demo.dto.chatbot.ChatbotResponseDTO;
import com.example.demo.enums.ChatbotSource;
import com.example.demo.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LocalChatbotFallbackServiceImpl implements ChatbotService {

    private static final double LOCAL_CONFIDENCE = 1.0;
    private static final double LOCAL_FALLBACK_CONFIDENCE = 0.35;
    private static final String RESTRICTED_MODULE_MESSAGE =
            "Este m\u00f3dulo est\u00e1 disponible solo para cuentas autorizadas. Tu cuenta no tiene acceso.";
    private static final String MODERATION_RESTRICTED_MESSAGE =
            "Moderaci\u00f3n es una funci\u00f3n para cuentas autorizadas. Tu cuenta no tiene acceso a este m\u00f3dulo.";
    private static final String LOOP_HINT =
            "Parece que seguimos en el mismo tema. También puedo ayudarte con otra sección de Artistlan.";

    private final ChatbotIntentActionMapper intentActionMapper;
    private final ChatbotReplyFinalizer replyFinalizer;

    @Override
    public ChatbotResponseDTO processMessage(ChatbotRequestDTO request) {
        String message = request != null ? request.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            return buildResponseForIntent("DEFAULT_FALLBACK", request);
        }

        return buildResponseForIntent(detectIntentForMessage(message), request);
    }

    public String detectIntentForMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "DEFAULT_FALLBACK";
        }
        return detectIntent(normalize(message));
    }

    public boolean shouldHandleLocally(String intent) {
        return intent != null && !intent.trim().isEmpty() && !"DEFAULT_FALLBACK".equals(intent.trim());
    }

    public ChatbotResponseDTO buildResponseForIntent(String intent, ChatbotRequestDTO request) {
        String safeIntent = intent != null && !intent.trim().isEmpty() ? intent.trim() : "DEFAULT_FALLBACK";
        double confidence = "DEFAULT_FALLBACK".equals(safeIntent) ? LOCAL_FALLBACK_CONFIDENCE : LOCAL_CONFIDENCE;
        String reply = "DEFAULT_FALLBACK".equals(safeIntent)
                ? "Escribe una pregunta sobre Artistlan y con gusto te ayudo."
                : replyFor(safeIntent);
        return buildResponse(safeIntent, reply, confidence, request);
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
        if (isStatisticsQuickActionIntent(text)) {
            return "ESTADISTICAS_ADMIN_ORIENTACION";
        }
        if (isAdminConvocatoriasQuickActionIntent(text)) {
            return "CONVOCATORIAS_ADMIN_ORIENTACION";
        }
        if (isAdminUsersQuickActionIntent(text)) {
            return "USUARIOS_ADMIN_ORIENTACION";
        }
        if (isModerationQuickActionIntent(text)) {
            return "MODERACION_REPORTES_ORIENTACION";
        }
        if (isArtworkDefinitionIntent(text)) {
            return "DEFINICION_OBRA";
        }
        if (isServiceDefinitionIntent(text)) {
            return "DEFINICION_SERVICIO";
        }
        if (isArtistDefinitionIntent(text)) {
            return "DEFINICION_ARTISTA";
        }
        if (isGoalDefinitionIntent(text)) {
            return "DEFINICION_META";
        }
        if (isRequestDefinitionIntent(text)) {
            return "DEFINICION_SOLICITUD";
        }
        if (isPortfolioDefinitionIntent(text)) {
            return "DEFINICION_PORTAFOLIO";
        }
        if (isConvocatoriaDefinitionIntent(text)) {
            return "DEFINICION_CONVOCATORIA";
        }
        if (isTransactionDefinitionIntent(text)) {
            return "DEFINICION_TRANSACCION";
        }
        if (isFavoriteDefinitionIntent(text)) {
            return "DEFINICION_FAVORITO";
        }
        if (isTwoFactorDefinitionIntent(text)) {
            return "DEFINICION_2FA";
        }
        if (isAccessProblemIntent(text)) {
            return "ACCESO_CUENTA_PROBLEMA";
        }
        if (isAdminStatisticsIntent(text)) {
            return "ESTADISTICAS_ADMIN_ORIENTACION";
        }
        if (isAdminConvocatoriasIntent(text)) {
            return "CONVOCATORIAS_ADMIN_ORIENTACION";
        }
        if (isAdminUsersIntent(text)) {
            return "USUARIOS_ADMIN_ORIENTACION";
        }
        if (isModerationIntent(text)) {
            return "MODERACION_REPORTES_ORIENTACION";
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
        if (isRequestsInboxIntent(text)) {
            return "SOLICITUDES_MENSAJES_ORIENTACION";
        }
        if (isFavoritesIntent(text)) {
            return "FAVORITOS_ORIENTACION";
        }
        if (isGoalsIntent(text)) {
            return "METAS_PERSONALES_ORIENTACION";
        }
        if (isExploreIntent(text)) {
            return "EXPLORAR_BUSCAR_CONTENIDO";
        }
        if (isPortfolioIntent(text)) {
            return "PORTAFOLIO_CONTENIDO_PROPIO";
        }
        if (containsAny(text, "contactar", "contacto", "mensaje al artista", "hablar con artista", "vendedor")) {
            return "CONTACTO_ARTISTA";
        }
        if (containsAny(text, "reportar", "reporto", "reporte", "denunciar", "contenido inapropiado")) {
            return "REPORTAR_CONTENIDO";
        }
        if (isAccountSecurityIntent(text)) {
            return "CUENTA_SEGURIDAD_PRIVACIDAD";
        }
        if (isPublicConvocatoriasIntent(text)) {
            return "CONVOCATORIAS_ORIENTACION";
        }
        if (containsAny(text, "ayuda", "como funciona", "que puedo hacer", "app", "artistlan")) {
            return "AYUDA_GENERAL_APP";
        }
        return "DEFAULT_FALLBACK";
    }

    private boolean isArtworkDefinitionIntent(String text) {
        return containsAny(text, "que es una obra", "que es obra", "definicion de obra");
    }

    private boolean isServiceDefinitionIntent(String text) {
        return containsAny(text, "que es un servicio", "que es servicio", "definicion de servicio");
    }

    private boolean isArtistDefinitionIntent(String text) {
        return containsAny(text, "que es un artista", "que es artista", "definicion de artista");
    }

    private boolean isGoalDefinitionIntent(String text) {
        return containsAny(text, "que es una meta", "que es meta", "definicion de meta");
    }

    private boolean isRequestDefinitionIntent(String text) {
        return containsAny(text, "que es una solicitud", "que es solicitud", "definicion de solicitud");
    }

    private boolean isPortfolioDefinitionIntent(String text) {
        return containsAny(text, "que es el portafolio", "que es portafolio", "definicion de portafolio");
    }

    private boolean isConvocatoriaDefinitionIntent(String text) {
        return containsAny(text, "que es una convocatoria", "que es convocatoria", "definicion de convocatoria");
    }

    private boolean isTransactionDefinitionIntent(String text) {
        return containsAny(text, "que es una transaccion", "que es transaccion", "definicion de transaccion");
    }

    private boolean isFavoriteDefinitionIntent(String text) {
        return containsAny(text, "que es favorito", "que es un favorito", "definicion de favorito");
    }

    private boolean isTwoFactorDefinitionIntent(String text) {
        return containsAny(text, "que es 2fa", "que es el 2fa", "que es doble factor", "definicion de 2fa");
    }

    private boolean isAccessProblemIntent(String text) {
        return containsAny(text,
                "recuperar acceso",
                "recuperar contrasena",
                "recuperar mi contrasena",
                "recuperar password",
                "recuperar clave",
                "recupero mi contrasena",
                "recupero contrasena",
                "olvide",
                "olvide mi contrasena",
                "no recuerdo mi contrasena",
                "no recuerdo la contrasena",
                "cambiar contrasena",
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
                "como uso el carrito",
                "paypal",
                "carrito",
                "pago",
                "pedido",
                "solicitar compra",
                "que pasa despues de comprar",
                "proceso de compra");
    }

    private boolean isTransactionsIntent(String text) {
        return containsViewIntentFor(text,
                "mis compras",
                "compras",
                "mis ventas",
                "ventas",
                "mis transacciones",
                "transacciones")
                || containsAny(text,
                "mis compras",
                "mis ventas",
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
                "cancelar solicitud",
                "como gestiono solicitudes");
    }

    private boolean isRequestsInboxIntent(String text) {
        return containsViewIntentFor(text,
                "mis solicitudes",
                "solicitudes",
                "solicitud",
                "mis notificaciones",
                "notificaciones",
                "notificacion",
                "mis mensajes",
                "mensajes",
                "bandeja")
                || containsAny(text,
                "solicitudes",
                "solicitud",
                "mensajes",
                "notificaciones",
                "notificacion",
                "bandeja");
    }

    private boolean isFavoritesIntent(String text) {
        return containsViewIntentFor(text,
                "favoritos",
                "mis favoritos")
                || containsAny(text,
                "mis favoritos",
                "guardar favorito",
                "quitar favorito",
                "obras favoritas",
                "servicios favoritos",
                "artistas favoritos");
    }

    private boolean isGoalsIntent(String text) {
        return containsViewIntentFor(text,
                "metas",
                "mis metas",
                "meta",
                "una meta",
                "progreso de una meta")
                || containsAny(text,
                "que son las metas personales",
                "para que sirven las metas",
                "como creo una meta",
                "como subo una meta",
                "como agrego una meta",
                "como actualizo una meta",
                "como cancelo una meta",
                "crear meta",
                "mis metas",
                "progreso de meta",
                "cancelar meta",
                "metas personales");
    }

    private boolean isAdminStatisticsIntent(String text) {
        return containsViewIntentFor(text,
                "estadisticas",
                "las estadisticas",
                "estadisticas de la plataforma")
                || containsAny(text,
                "quiero ver estadisticas",
                "quiero ver estadisticas de la plataforma",
                "estadisticas de la plataforma",
                "observaciones de estadisticas",
                "observacion de estadisticas",
                "como anoto una observacion de estadisticas",
                "como agrego una observacion de estadisticas",
                "como agrego una observacion en estadisticas",
                "como edito una observacion de estadisticas",
                "como elimino una observacion de estadisticas");
    }

    private boolean isStatisticsQuickActionIntent(String text) {
        return containsAny(text,
                "ver estadisticas",
                "ver estadisticas de la plataforma");
    }

    private boolean isAdminConvocatoriasIntent(String text) {
        return containsAny(text,
                "como edito una convocatoria",
                "como subo una convocatoria",
                "como creo una convocatoria",
                "como publico una convocatoria",
                "como elimino una convocatoria",
                "como actualizo una convocatoria",
                "edito convocatoria",
                "edito convocatorias",
                "quiero editar convocatoria",
                "quiero subir convocatorias",
                "quiero crear convocatorias",
                "quiero publicar convocatorias",
                "quiero editar una convocatoria",
                "quiero editar convocatorias",
                "quiero actualizar convocatoria",
                "actualizo convocatoria",
                "actualizo convocatorias",
                "elimino convocatoria",
                "elimino convocatorias",
                "quiero eliminar convocatorias",
                "quiero eliminar convocatoria",
                "borro convocatoria",
                "borro convocatorias",
                "quiero borrar convocatoria",
                "quiero borrar convocatorias",
                "subir convocatoria",
                "subir convocatorias",
                "subo convocatoria",
                "subo una convocatoria",
                "crear convocatoria",
                "crear convocatorias",
                "creo convocatoria",
                "creo una convocatoria",
                "publicar convocatoria",
                "publicar convocatorias",
                "publico convocatoria",
                "publico una convocatoria",
                "editar convocatoria",
                "editar convocatorias",
                "actualizar convocatoria",
                "actualizar convocatorias",
                "eliminar convocatoria",
                "eliminar convocatorias",
                "borrar convocatoria",
                "borrar convocatorias",
                "quitar convocatoria",
                "quitar convocatorias",
                "quito convocatoria",
                "quito convocatorias",
                "quitaria convocatoria",
                "quitaria convocatorias");
    }

    private boolean isAdminConvocatoriasQuickActionIntent(String text) {
        return containsAny(text, "editar convocatorias");
    }

    private boolean isPublicConvocatoriasIntent(String text) {
        return containsViewIntentFor(text,
                "convocatoria",
                "convocatorias",
                "una convocatoria")
                || containsAny(text,
                "convocatoria",
                "convocatorias",
                "quiero ver convocatoria",
                "quiero ver convocatorias",
                "donde estan las convocatorias",
                "evento",
                "calendario");
    }

    private boolean isPortfolioIntent(String text) {
        return containsViewIntentFor(text,
                "portafolio",
                "mis obras",
                "mis servicios")
                || containsAny(text,
                "portafolio",
                "mis obras",
                "mis servicios",
                "mi arte",
                "contenido propio");
    }

    private boolean isAccountSecurityIntent(String text) {
        return containsViewIntentFor(text,
                "perfil",
                "seguridad",
                "privacidad")
                || containsAny(text,
                "seguridad",
                "privacidad",
                "perfil",
                "datos",
                "contrasena",
                "2fa",
                "doble factor");
    }

    private boolean isAdminUsersIntent(String text) {
        return containsAny(text,
                "como cambio roles",
                "como cambio el rol de un usuario",
                "como gestiono usuarios",
                "como suspendo un usuario",
                "como reactivo un usuario",
                "gestionar usuarios",
                "gestion de usuarios");
    }

    private boolean isAdminUsersQuickActionIntent(String text) {
        return containsAny(text, "gestionar usuarios");
    }

    private boolean isModerationIntent(String text) {
        return containsAny(text,
                "como veo un reporte",
                "como tomo un reporte",
                "como soluciono un reporte",
                "como resuelvo un reporte",
                "como atiendo un reporte",
                "como cierro un reporte",
                "quiero moderar reportes",
                "moderacion",
                "moderacion reportes",
                "ver reportes",
                "tomar reporte",
                "resolver reporte");
    }

    private boolean isModerationQuickActionIntent(String text) {
        return containsAny(text, "moderacion", "moderacion reportes");
    }

    private boolean isAdminRestrictedIntent(String text) {
        return containsAny(text,
                "administracion",
                "panel admin");
    }

    private boolean isExploreIntent(String text) {
        return containsViewIntentFor(text,
                "obras",
                "obra",
                "una obra",
                "servicios",
                "servicio",
                "un servicio",
                "artistas",
                "artista",
                "un artista",
                "arte")
                || containsAny(text,
                "como ver artistas",
                "como ver servicios",
                "como ver obras",
                "como busco artistas",
                "como busco servicios",
                "como busco obras",
                "quiero ver artistas",
                "quiero ver servicios",
                "quiero ver obras",
                "explorar obras",
                "buscar obras",
                "encontrar obras",
                "explorar servicios",
                "buscar servicios",
                "encontrar servicios",
                "explorar artistas",
                "buscar artistas",
                "encontrar artistas",
                "explorar contenido",
                "buscar contenido",
                "encontrar contenido",
                "quiero ver arte");
    }

    private String replyFor(String intent) {
        return switch (intent) {
            case "ACCESO_CUENTA_PROBLEMA" -> stepReply(
                    "Para recuperar tu contraseña en Artistlan:",
                    "Entra a la opción de recuperar contraseña.",
                    "Escribe tu nombre de usuario o el dato que te pida la pantalla.",
                    "Revisa el código enviado al correo asociado y valídalo.",
                    "Elige una nueva contraseña y vuelve a iniciar sesión."
            );
            case "PUBLICAR_CONTENIDO" -> stepReply(
                    "Para publicar contenido en Artistlan:",
                    "Elige si vas a subir una obra o un servicio.",
                    "Completa los datos solicitados en la pantalla correspondiente.",
                    "Revisa la información antes de publicar.",
                    "Confirma y verifica que aparezca en tu portafolio."
            );
            case "PUBLICAR_OBRA_GUIA" -> stepReply(
                    "Para subir una obra:",
                    "Entra a Portafolio o a la opción de subir obra.",
                    "Completa los datos de la obra.",
                    "Agrega imagen, categoría, precio y estado.",
                    "Publica la obra y revisa que aparezca en tu perfil o portafolio."
            );
            case "PUBLICAR_SERVICIO_GUIA" -> stepReply(
                    "Para subir un servicio:",
                    "Entra a la opción de subir servicio.",
                    "Completa la descripción y los datos principales.",
                    "Agrega precio, detalles y la información necesaria.",
                    "Publica y verifica que aparezca en tu portafolio."
            );
            case "DEFINICION_OBRA" -> "Una obra es una publicación artística dentro de Artistlan. Puede incluir imagen, datos, categoría, estado y precio, según como la registre el artista.";
            case "DEFINICION_SERVICIO" -> "Un servicio es una publicación donde un artista ofrece una actividad o apoyo relacionado con arte, como encargos, asesorías, clases o trabajos personalizados, según lo que permita la app.";
            case "DEFINICION_ARTISTA" -> "Un artista es un usuario que publica obras o servicios dentro de Artistlan. Puedes ver su perfil público, explorar su contenido y seguir interactuando con sus publicaciones según las opciones disponibles.";
            case "DEFINICION_META" -> "Una meta personal es un objetivo que el usuario registra para dar seguimiento a su avance dentro de Artistlan.";
            case "DEFINICION_SOLICITUD" -> "Una solicitud es una acción que conecta a usuarios dentro de un flujo de Artistlan, por ejemplo para compras o gestiones relacionadas, según la opción disponible en la app.";
            case "DEFINICION_PORTAFOLIO" -> "El portafolio es la sección donde puedes revisar el contenido que has publicado en Artistlan, como obras o servicios.";
            case "DEFINICION_CONVOCATORIA" -> "Una convocatoria es una oportunidad o anuncio publicado dentro de Artistlan para informar actividades, eventos o participaciones disponibles.";
            case "DEFINICION_TRANSACCION" -> "Una transacción es el registro de una compra, venta o movimiento relacionado que puedes consultar desde la sección correspondiente de Artistlan.";
            case "DEFINICION_FAVORITO" -> "Un favorito es un contenido o perfil que guardas para revisarlo más tarde dentro de Artistlan.";
            case "DEFINICION_2FA" -> "2FA es la verificación en dos pasos. Sirve para agregar una capa extra de seguridad a tu cuenta al pedir un código adicional al iniciar sesión o confirmar acciones.";
            case "PORTAFOLIO_CONTENIDO_PROPIO" -> stepReply(
                    "Para revisar tu portafolio:",
                    "Entra a tu portafolio desde la app.",
                    "Revisa tus obras y servicios publicados.",
                    "Abre el contenido que quieras consultar o actualizar.",
                    "Si necesitas publicar algo nuevo, usa la opción de subir contenido."
            );
            case "EXPLORAR_BUSCAR_CONTENIDO" -> stepReply(
                    "Para ver contenido publicado:",
                    "Entra a Explorar.",
                    "Elige si quieres ver obras, servicios o artistas.",
                    "Usa búsqueda o filtros para encontrar contenido.",
                    "Abre una tarjeta para ver más detalles."
            );
            case "CONTACTO_ARTISTA" -> stepReply(
                    "Para contactar a un artista:",
                    "Entra a Explorar y abre la obra, servicio o perfil que te interese.",
                    "Revisa las opciones de contacto o mensajes disponibles.",
                    "Envía tu mensaje o sigue el flujo que muestre la app.",
                    "Da seguimiento desde mensajes o solicitudes si aplica."
            );
            case "COMPRA_CARRITO_ORIENTACION" -> stepReply(
                    "Para comprar una obra o usar carrito y PayPal:",
                    "Explora las obras disponibles y abre la que te interesa.",
                    "Envía o gestiona la solicitud de compra según el flujo disponible.",
                    "Si la compra pasa a carrito o reserva, revisa esa sección antes de pagar.",
                    "Finaliza el pago con PayPal cuando esté disponible y revisa el estado en Mis transacciones."
            );
            case "TRANSACCIONES_SEGUIMIENTO" -> stepReply(
                    "Para ver tus compras, ventas o transacciones:",
                    "Entra a Mis transacciones.",
                    "Revisa la lista de compras o ventas disponibles.",
                    "Abre la transacción que quieras consultar.",
                    "Verifica su estado y el detalle mostrado por la app."
            );
            case "SOLICITUDES_GESTION" -> stepReply(
                    "Para aceptar, rechazar o cancelar solicitudes:",
                    "Entra a Solicitudes.",
                    "Abre la sección de recibidas o enviadas según tu caso.",
                    "Selecciona la solicitud que quieras revisar.",
                    "Usa la opción de aceptar, rechazar o cancelar si el estado lo permite."
            );
            case "SOLICITUDES_MENSAJES_ORIENTACION" -> stepReply(
                    "Para ver solicitudes, mensajes o notificaciones:",
                    "Entra a Solicitudes o al centro de mensajes.",
                    "Abre la pestaña de recibidas, enviadas, mensajes o notificaciones.",
                    "Selecciona el elemento que quieras revisar.",
                    "Da seguimiento desde esa misma sección según la acción disponible."
            );
            case "FAVORITOS_ORIENTACION" -> stepReply(
                    "Para revisar tus favoritos:",
                    "Entra a Favoritos.",
                    "Revisa las obras, servicios o artistas guardados.",
                    "Abre el contenido que quieras consultar.",
                    "Si quieres guardar algo nuevo, hazlo desde Explorar cuando la opción esté disponible."
            );
            case "METAS_PERSONALES_ORIENTACION" -> stepReply(
                    "Las metas personales sirven para organizar objetivos dentro de Artistlan, dar seguimiento al progreso y consultar su estado desde Mis metas.",
                    "Entra a Mis metas.",
                    "Crea o revisa una meta.",
                    "Completa los datos solicitados.",
                    "Da seguimiento a su progreso."
            );
            case "REPORTAR_CONTENIDO" -> stepReply(
                    "Para reportar contenido:",
                    "Entra a Explorar y abre la obra o servicio que quieras reportar.",
                    "Busca la opción de reportar o denunciar.",
                    "Completa la información solicitada.",
                    "Envía el reporte y revisa si la app muestra confirmación."
            );
            case "CUENTA_SEGURIDAD_PRIVACIDAD" -> stepReply(
                    "Para editar tu perfil o revisar seguridad de cuenta:",
                    "Entra a tu perfil.",
                    "Abre la sección de datos o seguridad.",
                    "Actualiza tu información, cambia tu contraseña o gestiona 2FA según la opción disponible.",
                    "Guarda los cambios y confirma el nuevo estado de tu cuenta."
            );
            case "CONVOCATORIAS_ORIENTACION" -> stepReply(
                    "Para ver convocatorias:",
                    "Entra a Convocatorias.",
                    "Revisa las oportunidades o anuncios disponibles.",
                    "Abre la convocatoria que te interese.",
                    "Consulta sus datos y da seguimiento desde esa sección."
            );
            case "ESTADISTICAS_ADMIN_ORIENTACION" -> replyForStatisticsIntent();
            case "CONVOCATORIAS_ADMIN_ORIENTACION" -> replyForAdminConvocatoriasIntent();
            case "USUARIOS_ADMIN_ORIENTACION" -> replyForAdminUsersIntent();
            case "MODERACION_REPORTES_ORIENTACION" -> replyForModerationIntent();
            case "ADMIN_RESTRINGIDO_ORIENTACION" -> "Este módulo está disponible solo para cuentas autorizadas. Tu cuenta no tiene acceso.";
            case "AYUDA_GENERAL_APP" -> "Puedo orientarte sobre obras, servicios, artistas, compras, solicitudes, convocatorias, metas, portafolio y seguridad de cuenta.";
            case "SMALLTALK_GRACIAS" -> "Con gusto. Estoy aquí para ayudarte con Artistlan.";
            case "SMALLTALK_DESPEDIDA" -> "Hasta luego. Cuando necesites ayuda con Artistlan, vuelve a escribirme.";
            case "DEFAULT_WELCOME" -> "Hola, soy el asistente de Artistlan. Puedo ayudarte con obras, servicios, artistas, compras, solicitudes, convocatorias, metas, portafolio y tu cuenta.";
            default -> "No estoy seguro de haber entendido. Puedes preguntarme, por ejemplo: cómo veo artistas, qué es una obra o cómo recupero mi contraseña.";
        };
    }

    private String replyForStatisticsIntent() {
        if (!hasRole("ADMIN")) {
            return RESTRICTED_MODULE_MESSAGE;
        }
        if (hasRole("ADMIN")) {
            return stepReply(
                    "Para revisar las estad\u00edsticas de la plataforma:",
                    "Abre el men\u00fa lateral.",
                    "Entra a Administraci\u00f3n.",
                    "Selecciona Estad\u00edsticas de la plataforma.",
                    "Revisa las secciones disponibles.",
                    "Si necesitas observaciones, usa la opci\u00f3n correspondiente dentro del m\u00f3dulo."
            );
        }
        if (!hasRole("ADMIN")) {
            return "Este mÃ³dulo estÃ¡ disponible solo para cuentas autorizadas. Tu cuenta no tiene acceso.";
        }
        if (hasRole("ADMIN")) {
            return stepReply(
                    "Para revisar las estadísticas de la plataforma:",
                    "Abre el menú lateral.",
                    "Entra a Administración.",
                    "Selecciona Estadísticas de la plataforma.",
                    "Revisa las secciones disponibles.",
                    "Si necesitas observaciones, usa la opción correspondiente dentro del módulo."
            );
        }
        return "Esta es una función administrativa. Tu cuenta no tiene acceso a este módulo.";
    }

    private String replyForAdminConvocatoriasIntent() {
        if (!hasRole("ADMIN")) {
            return RESTRICTED_MODULE_MESSAGE;
        }
        if (hasRole("ADMIN")) {
            return stepReply(
                    "Para gestionar convocatorias:",
                    "Abre el men\u00fa lateral.",
                    "Entra a Administraci\u00f3n.",
                    "Selecciona Editar convocatorias.",
                    "Crea, edita, publica o elimina convocatorias seg\u00fan lo necesites."
            );
        }
        if (hasRole("ADMIN")) {
            return stepReply(
                    "Para gestionar convocatorias:",
                    "Abre el menú lateral.",
                    "Entra a Administración.",
                    "Selecciona Editar convocatorias.",
                    "Crea, edita, publica o elimina convocatorias según lo necesites."
            );
        }
        return "Este módulo está disponible solo para cuentas autorizadas. Tu cuenta no tiene acceso.";
    }

    private String replyForAdminUsersIntent() {
        if (!hasRole("ADMIN")) {
            return RESTRICTED_MODULE_MESSAGE;
        }
        if (hasRole("ADMIN")) {
            return stepReply(
                    "Para gestionar usuarios:",
                    "Abre el men\u00fa lateral.",
                    "Entra a Administraci\u00f3n.",
                    "Selecciona Gestionar usuarios.",
                    "Busca el usuario y usa las acciones disponibles."
            );
        }
        if (hasRole("ADMIN")) {
            return stepReply(
                    "Para gestionar usuarios:",
                    "Abre el menú lateral.",
                    "Entra a Administración.",
                    "Selecciona Gestionar usuarios.",
                    "Busca el usuario y usa las acciones disponibles."
            );
        }
        return "Esta es una función administrativa. Tu cuenta no tiene acceso a este módulo.";
    }

    private String replyForModerationIntent() {
        if (!hasRole("ADMIN") && !hasRole("MODERADOR")) {
            return MODERATION_RESTRICTED_MESSAGE;
        }
        if (hasRole("ADMIN") || hasRole("MODERADOR")) {
            return stepReply(
                    "Para revisar reportes en moderaci\u00f3n:",
                    "Abre el men\u00fa lateral.",
                    "Entra a Administraci\u00f3n.",
                    "Selecciona Moderaci\u00f3n.",
                    "Abre un reporte para revisar su detalle.",
                    "Usa las acciones disponibles para tomarlo o resolverlo."
            );
        }
        if (hasRole("ADMIN") || hasRole("MODERADOR")) {
            return stepReply(
                    "Para revisar reportes en moderación:",
                    "Abre el menú lateral.",
                    "Entra a Administración.",
                    "Selecciona Moderación.",
                    "Abre un reporte para revisar su detalle.",
                    "Usa las acciones disponibles para tomarlo o resolverlo."
            );
        }
        return "Moderación es una función para cuentas autorizadas. Tu cuenta no tiene acceso a este módulo.";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsViewIntentFor(String text, String... targets) {
        String[] prefixes = {
                "como veo ",
                "donde veo ",
                "como consulto ",
                "consultar ",
                "como checo ",
                "checar ",
                "como reviso ",
                "revisar ",
                "ver "
        };

        for (String target : targets) {
            for (String prefix : prefixes) {
                if (text.contains(prefix + target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || role == null || role.trim().isEmpty()) {
            return false;
        }
        String expectedAuthority = "ROLE_" + role.trim().toUpperCase(Locale.ROOT);
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expectedAuthority.equalsIgnoreCase(authority.getAuthority()));
    }

    private String stepReply(String intro, String... steps) {
        StringBuilder reply = new StringBuilder(intro);
        for (int i = 0; i < steps.length; i++) {
            reply.append("\n").append(i + 1).append(". ").append(steps[i]);
        }
        return reply.toString();
    }
}
