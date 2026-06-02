package com.example.demo.service.impl;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class ChatbotReplyFinalizer {

    private static final String DEFAULT_CLOSING = "Necesitas ayuda con algo mas?";

    public String withNaturalClosing(String reply) {
        if (reply == null || reply.trim().isEmpty()) {
            return DEFAULT_CLOSING;
        }

        String trimmed = reply.trim();
        String normalized = normalize(trimmed);
        if (hasClosing(normalized) || normalized.contains("hasta luego") || normalized.contains("adios")) {
            return trimmed;
        }

        return trimmed + "\n\n" + DEFAULT_CLOSING;
    }

    private boolean hasClosing(String normalized) {
        return normalized.contains("necesitas ayuda")
                || normalized.contains("algo mas")
                || normalized.contains("otra seccion")
                || normalized.contains("otro modulo")
                || normalized.contains("puedo ayudarte con otra");
    }

    private String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }
}
