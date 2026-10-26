package com.example.protec.maquinas;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import com.example.protec.consentimentos.ConsentimentosRepository;

@Component
public class MaquinaGateway extends TextWebSocketHandler {

    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CONSENT_VALIDITY = Duration.ofHours(1);

    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>(); // maquinaId -> open connection
    private final Map<UUID, Instant> lastSeen = new ConcurrentHashMap<>(); // maquinaId -> last message from the Pi
    private final Map<UUID, Boolean> lastSentState = new ConcurrentHashMap<>(); // maquinaId -> last command sent

    private final MaquinasRepository maquinasRepository;
    private final ConsentimentosRepository consentimentosRepository;
    private final MaquinasService maquinasService;

    public MaquinaGateway(MaquinasRepository maquinasRepository,
            ConsentimentosRepository consentimentosRepository,
            MaquinasService maquinasService) {
        this.maquinasRepository = maquinasRepository;
        this.consentimentosRepository = consentimentosRepository;
        this.maquinasService = maquinasService;
    }

    // Pi connects at: ws://API:8081/ws/pi?maquinaId=<uuid>
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID maquinaId = extractMaquinaId(session);
        if (maquinaId == null || !maquinasRepository.existsById(maquinaId)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        WebSocketSession current = sessions.get(maquinaId);
        if (current != null && current.isOpen() && isOnline(maquinaId)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("maquina already in use"));
            return;
        }

        sessions.put(maquinaId, session);
        session.getAttributes().put("maquinaId", maquinaId);
        lastSeen.put(maquinaId, Instant.now());
        lastSentState.remove(maquinaId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        UUID maquinaId = (UUID) session.getAttributes().get("maquinaId");
        if (maquinaId == null) {
            return;
        }
        lastSeen.put(maquinaId, Instant.now());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID maquinaId = (UUID) session.getAttributes().get("maquinaId");
        if (maquinaId == null) {
            return;
        }
        if (sessions.remove(maquinaId, session)) {
            lastSeen.remove(maquinaId);
            lastSentState.remove(maquinaId);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void reconcile() {
        for (UUID maquinaId : sessions.keySet()) {
            WebSocketSession session = sessions.get(maquinaId);
            if (session == null) {
                continue;
            }

            if (!isOnline(maquinaId)) {
                closeQuietly(session);
                continue;
            }

            boolean shouldBeEnabled = hasValidConsent(maquinaId);
            Boolean alreadySent = lastSentState.get(maquinaId);
            if (alreadySent != null && alreadySent == shouldBeEnabled) {
                continue;
            }

            if (send(session, shouldBeEnabled)) {
                lastSentState.put(maquinaId, shouldBeEnabled);
            }
        }
    }

    public List<MaquinasResponse> findAvailable() {
        List<MaquinasResponse> result = new ArrayList<>();
        for (Maquinas maquina : maquinasRepository.findAll()) {
            if (!isOnline(maquina.getId())) {
                result.add(maquinasService.toResponse(maquina));
            }
        }
        return result;
    }

    private boolean hasValidConsent(UUID maquinaId) {
        boolean isAtiva = maquinasRepository.findById(maquinaId)
                .map(Maquinas::getAtividade)
                .orElse(false);

        if (!isAtiva) {
            return false;
        }

        return consentimentosRepository.findFirstByMaquinaIdOrderByDataDesc(maquinaId)
                .map(c -> c.getData().plus(CONSENT_VALIDITY).isAfter(Instant.now()))
                .orElse(false);
    }

    private boolean isOnline(UUID maquinaId) {
        Instant seen = lastSeen.get(maquinaId);
        if (seen == null) {
            return false;
        }
        return seen.isAfter(Instant.now().minus(HEARTBEAT_TIMEOUT));
    }

    private boolean send(WebSocketSession session, boolean enabled) {
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage("{\"type\":\"SET\",\"enabled\":" + enabled + "}"));
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }

    private UUID extractMaquinaId(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        String raw = UriComponentsBuilder.fromUri(session.getUri())
                .build().getQueryParams().getFirst("maquinaId");
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}