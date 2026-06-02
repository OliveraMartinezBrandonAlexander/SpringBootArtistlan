package com.example.demo.service.impl;

import com.example.demo.service.GeminiChatbotFallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GeminiChatbotFallbackServiceImpl implements GeminiChatbotFallbackService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String MODEL_PREFIX = "models/";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String SYSTEM_PROMPT = """
            Eres el asistente de ayuda de Artistlan, una aplicacion para publicar, explorar, vender/comprar obras artisticas, ofrecer servicios, gestionar portafolio, solicitudes, carrito, favoritos, transacciones, convocatorias, metas personales, perfil y ayuda general. Responde solo sobre funciones de Artistlan. Si no sabes algo, dilo de forma amable y ofrece opciones generales. No inventes pantallas, rutas, permisos, precios, politicas ni funciones. No afirmes que realizaste acciones dentro de la app. No generes botones, actions, IDs, rutas ni permisos. Responde breve, claro y en espanol mexicano natural. Termina con una pregunta de continuidad como: Necesitas ayuda con algo mas?
            """;

    private final String apiKey;
    private final String configuredModel;
    private final String modelForUrl;
    private final String endpointUrl;
    private final RestClient restClient;
    private final ChatbotReplyFinalizer replyFinalizer;

    public GeminiChatbotFallbackServiceImpl(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
            ChatbotReplyFinalizer replyFinalizer
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.configuredModel = normalizeConfiguredModel(model);
        this.modelForUrl = normalizeModelForUrl(configuredModel);
        this.endpointUrl = BASE_URL + "/models/" + modelForUrl + ":generateContent";
        this.replyFinalizer = replyFinalizer;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory())
                .build();

        log.info("Gemini modelo configurado: {}", modelForUrl);
        log.info("Gemini endpoint usado: {}", endpointUrl);
    }

    @Override
    public Optional<String> generateReply(String message) {
        if (!isEnabled() || message == null || message.trim().isEmpty()) {
            return Optional.empty();
        }

        String safeMessage = limitMessage(message.trim());
        try {
            GeminiGenerateResponse response = restClient.post()
                    .uri("/models/{model}:generateContent", modelForUrl)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildRequest(safeMessage))
                    .retrieve()
                    .body(GeminiGenerateResponse.class);

            return extractText(response)
                    .map(replyFinalizer::withNaturalClosing);
        } catch (RestClientResponseException ex) {
            logGeminiHttpFailure(ex);
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("Gemini no disponible; usando fallback local: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Gemini no disponible; usando fallback local: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private boolean isEnabled() {
        return !apiKey.isBlank();
    }

    private GeminiGenerateRequest buildRequest(String message) {
        String prompt = SYSTEM_PROMPT + "\n\nUsuario: " + message;
        return new GeminiGenerateRequest(
                List.of(new GeminiContent(List.of(new GeminiPart(prompt))))
        );
    }

    private Optional<String> extractText(GeminiGenerateResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return Optional.empty();
        }

        GeminiCandidate candidate = response.candidates().get(0);
        if (candidate == null || candidate.content() == null || candidate.content().parts() == null) {
            return Optional.empty();
        }

        StringBuilder text = new StringBuilder();
        for (GeminiPart part : candidate.content().parts()) {
            if (part != null && part.text() != null && !part.text().trim().isEmpty()) {
                if (!text.isEmpty()) {
                    text.append("\n");
                }
                text.append(part.text().trim());
            }
        }

        String reply = text.toString().trim();
        return reply.isEmpty() ? Optional.empty() : Optional.of(reply);
    }

    private void logGeminiHttpFailure(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == 404) {
            log.warn("Modelo no encontrado o URL incorrecta; usando fallback local. Endpoint: {}", endpointUrl);
        } else if (status == 429) {
            log.warn("Limite/cuota de Gemini alcanzado; usando fallback local");
        } else if (status == 401 || status == 403) {
            log.warn("API key no autorizada o sin permisos; usando fallback local");
        } else {
            log.warn("Gemini no disponible; usando fallback local. HTTP {}", status);
        }
    }

    private String normalizeConfiguredModel(String model) {
        String normalized = model != null ? model.trim() : "";
        return normalized.isEmpty() ? DEFAULT_MODEL : normalized;
    }

    private String normalizeModelForUrl(String rawModel) {
        String normalized = rawModel != null ? rawModel.trim() : "";
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith(MODEL_PREFIX)) {
            normalized = normalized.substring(MODEL_PREFIX.length());
        }
        return normalized.isEmpty() ? DEFAULT_MODEL : normalized;
    }

    private String limitMessage(String message) {
        return message.length() <= 1200 ? message : message.substring(0, 1200);
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(12));
        return factory;
    }

    private record GeminiGenerateRequest(List<GeminiContent> contents) {
    }

    private record GeminiContent(List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }

    private record GeminiGenerateResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }
}
