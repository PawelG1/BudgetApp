package pk.gp.pasir_galusza_pawel.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class GroupNotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GroupNotificationWebSocketHandler.class);

    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String email = (String) session.getAttributes().get("wsUserEmail");
        if (email != null) {
            sessions.computeIfAbsent(email, k -> new CopyOnWriteArrayList<>()).add(session);
            log.info("[WS] Połączenie nawiązane – użytkownik: {} (sesja: {}, łącznie sesji: {})",
                    email, session.getId(), sessions.get(email).size());
        } else {
            log.warn("[WS] Połączenie bez tokenu – zamykam sesję: {}", session.getId());
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = (String) session.getAttributes().get("wsUserEmail");
        if (email != null) {
            List<WebSocketSession> userSessions = sessions.get(email);
            if (userSessions != null) {
                userSessions.remove(session);
                if (userSessions.isEmpty()) {
                    sessions.remove(email);
                }
            }
            log.info("[WS] Połączenie zamknięte – użytkownik: {} | status: {}", email, status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("[WS] Odebrano wiadomość od klienta (ignoruję): {}", message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[WS] Błąd transportu – sesja: {} | {}", session.getId(), exception.getMessage());
    }

    public void sendToUser(String email, Object payload) {
        List<WebSocketSession> userSessions = sessions.get(email);
        if (userSessions == null || userSessions.isEmpty()) {
            log.warn("[WS] Brak aktywnej sesji dla: {} – wiadomość nie zostanie dostarczona", email);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);
            int sent = 0;
            for (WebSocketSession session : userSessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                    sent++;
                }
            }
            log.info("[WS] Powiadomienie dostarczone do: {} ({} sesji)", email, sent);
        } catch (Exception e) {
            log.error("[WS] Błąd wysyłania do: {} – {}", email, e.getMessage());
        }
    }

    public boolean isConnected(String email) {
        List<WebSocketSession> userSessions = sessions.get(email);
        return userSessions != null && userSessions.stream().anyMatch(WebSocketSession::isOpen);
    }
}
