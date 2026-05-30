package com.example.demo.service.impl;

import com.example.demo.config.DialogflowConfig;
import com.example.demo.dto.chatbot.ChatbotRequestDTO;
import com.example.demo.dto.chatbot.ChatbotResponseDTO;
import com.example.demo.enums.ChatbotSource;
import com.example.demo.service.ChatbotService;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class DialogflowChatbotServiceImpl implements ChatbotService {

    private final DialogflowConfig dialogflowConfig;
    private final ChatbotIntentActionMapper intentActionMapper;
    private final LocalChatbotFallbackServiceImpl localFallbackService;

    private final Object clientLock = new Object();
    private volatile SessionsClient sessionsClient;

    @Override
    public ChatbotResponseDTO processMessage(ChatbotRequestDTO request) {
        String message = request != null ? request.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            return localFallbackService.processMessage(request);
        }

        if (!dialogflowConfig.isEnabled()) {
            return localFallbackService.processMessage(request);
        }

        try {
            DetectIntentResponse response = detectIntent(request, message.trim());
            QueryResult queryResult = response.getQueryResult();
            String reply = queryResult.getFulfillmentText();

            if (reply == null || reply.trim().isEmpty()) {
                return localFallbackService.processMessage(request);
            }

            String intent = normalizeIntentName(queryResult);
            if (intent == null || intent.isBlank()) {
                return localFallbackService.processMessage(request);
            }

            return ChatbotResponseDTO.builder()
                    .reply(reply.trim())
                    .intent(intent)
                    .source(ChatbotSource.DIALOGFLOW.name())
                    .confidence((double) queryResult.getIntentDetectionConfidence())
                    .quickReplies(intentActionMapper.quickRepliesFor(intent))
                    .actions(intentActionMapper.actionsFor(intent))
                    .build();
        } catch (Exception ex) {
            log.warn("No fue posible consultar Dialogflow. Se usará fallback local: {}", ex.getClass().getSimpleName());
            return localFallbackService.processMessage(request);
        }
    }

    private DetectIntentResponse detectIntent(ChatbotRequestDTO request, String message) throws IOException {
        String sessionId = resolveSessionId(request);
        SessionName sessionName = SessionName.of(dialogflowConfig.getProjectId(), sessionId);

        TextInput textInput = TextInput.newBuilder()
                .setText(message)
                .setLanguageCode(dialogflowConfig.getLanguageCode())
                .build();

        QueryInput queryInput = QueryInput.newBuilder()
                .setText(textInput)
                .build();

        DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder()
                .setSession(sessionName.toString())
                .setQueryInput(queryInput)
                .build();

        return getSessionsClient().detectIntent(detectIntentRequest);
    }

    private SessionsClient getSessionsClient() throws IOException {
        SessionsClient currentClient = sessionsClient;
        if (currentClient != null) {
            return currentClient;
        }

        synchronized (clientLock) {
            if (sessionsClient == null) {
                sessionsClient = dialogflowConfig.createSessionsClient();
            }
            return sessionsClient;
        }
    }

    private String resolveSessionId(ChatbotRequestDTO request) {
        String rawSessionId = request != null ? request.getSessionId() : null;
        if (rawSessionId == null || rawSessionId.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }

        String safeSessionId = rawSessionId.trim().replaceAll("[^A-Za-z0-9_.-]", "-");
        if (safeSessionId.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return safeSessionId.length() <= 36 ? safeSessionId : safeSessionId.substring(0, 36);
    }

    private String normalizeIntentName(QueryResult queryResult) {
        if (queryResult == null || !queryResult.hasIntent()) {
            return "DEFAULT_FALLBACK";
        }

        String displayName = queryResult.getIntent().getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            return "DEFAULT_FALLBACK";
        }

        String normalized = displayName.trim();
        if ("Default Welcome Intent".equalsIgnoreCase(normalized)) {
            return "DEFAULT_WELCOME";
        }
        if ("Default Fallback Intent".equalsIgnoreCase(normalized)) {
            return "DEFAULT_FALLBACK";
        }
        return normalized;
    }

    @PreDestroy
    public void closeSessionsClient() {
        SessionsClient currentClient = sessionsClient;
        if (currentClient != null) {
            currentClient.close();
        }
    }
}
