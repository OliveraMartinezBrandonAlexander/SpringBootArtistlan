package com.example.demo.config;

import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Getter
@Component
public class DialogflowConfig {

    private final String projectId;
    private final String languageCode;
    private final String region;
    private final boolean enabled;

    public DialogflowConfig(
            @Value("${artistlan.dialogflow.project-id:artistlan-f786b}") String projectId,
            @Value("${artistlan.dialogflow.language-code:es}") String languageCode,
            @Value("${artistlan.dialogflow.region:global}") String region,
            @Value("${artistlan.dialogflow.enabled:true}") boolean enabled
    ) {
        this.projectId = normalize(projectId, "artistlan-f786b");
        this.languageCode = normalize(languageCode, "es");
        this.region = normalize(region, "global");
        this.enabled = enabled;
    }

    public SessionsClient createSessionsClient() throws IOException {
        SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
        if (!isGlobalRegion()) {
            settingsBuilder.setEndpoint(region + "-dialogflow.googleapis.com:443");
        }
        return SessionsClient.create(settingsBuilder.build());
    }

    public boolean isGlobalRegion() {
        return "global".equalsIgnoreCase(region);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
