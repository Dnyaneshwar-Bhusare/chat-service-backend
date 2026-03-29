package com.codingworld.service1.websocket;

import com.codingworld.service1.service.UserPresenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // tracks last pong received time (ms) per sessionId
    private final Map<String, Long> lastPongTime = new ConcurrentHashMap<>();

    private static final long HEARTBEAT_INTERVAL_MS = 30_000;  // 30 seconds
    private static final long PONG_TIMEOUT_MS        = 60_000;  // 60 seconds — if no pong, session is stale

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserPresenceService userPresenceService;

    @PostConstruct
    public void init() {
        // Register presence broadcast for users connected on /chat endpoint
        userPresenceService.addPresenceChangeListener(this::broadcastPresenceChange);
        System.out.println("[ChatWS] Presence change listener registered.");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        lastPongTime.put(session.getId(), System.currentTimeMillis());
        System.out.println("[ChatWS] Connection established: " + session.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // Handle pong frames from client
        if (message instanceof PongMessage) {
            lastPongTime.put(session.getId(), System.currentTimeMillis());
            System.out.println("[ChatWS] Pong received from session: " + session.getId());
            return;
        }

        String payload = message.getPayload().toString();
        try {
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String type = (String) messageData.get("type");

            if ("login".equals(type)) {
                String userId = (String) messageData.get("userId");
                if (userId != null) {
                    WebSocketSession oldSession = userSessions.get(userId);
                    if (oldSession != null && !oldSession.getId().equals(session.getId())) {
                        System.out.println("[ChatWS] Replacing stale session for user: " + userId);
                        closeSession(oldSession, CloseStatus.SESSION_NOT_RELIABLE);
                    }

                    userSessions.put(userId, session);
                    userPresenceService.markOnline(userId, session.getId()); // pass sessionId

                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "login_success");
                    response.put("message", "Connected successfully");
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    System.out.println("[ChatWS] User logged in via WebSocket: " + userId);
                }
            } else if ("ping".equals(type)) {
                // Handle application-level ping from client
                lastPongTime.put(session.getId(), System.currentTimeMillis());
                Map<String, Object> pong = new HashMap<>();
                pong.put("type", "pong");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
            }
        } catch (Exception e) {
            System.err.println("[ChatWS] Error handling message: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String cause = exception.getMessage();
        if (cause != null && cause.contains("Connection reset by peer")) {
            System.out.println("[ChatWS] Client disconnected abruptly (Connection reset by peer) on session: " + session.getId());
        } else {
            System.err.println("[ChatWS] Transport error on session " + session.getId() + ": " + cause);
        }
        // Session is already broken — only clean up, do NOT try to close it
        removeSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        System.out.println("[ChatWS] Connection closed: " + session.getId() + " | status: " + closeStatus);
        removeSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Heartbeat job — runs every 30 seconds.
     * Sends a WebSocket-level ping to all connected sessions.
     * Sessions that haven't responded with a pong within PONG_TIMEOUT_MS are considered stale and closed.
     */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeat() {
        long now = System.currentTimeMillis();
        userSessions.forEach((userId, session) -> {
            if (!session.isOpen()) {
                System.out.println("[ChatWS] Heartbeat: session closed for user " + userId + ", cleaning up.");
                removeSession(session);
                return;
            }

            long lastPong = lastPongTime.getOrDefault(session.getId(), now);
            if (now - lastPong > PONG_TIMEOUT_MS) {
                System.err.println("[ChatWS] Heartbeat: no pong from user " + userId + " for " + ((now - lastPong) / 1000) + "s. Closing stale session.");
                removeSession(session);
                closeSession(session, CloseStatus.SESSION_NOT_RELIABLE);
                return;
            }

            try {
                // Send WebSocket-level ping frame
                session.sendMessage(new PingMessage());
                System.out.println("[ChatWS] Heartbeat ping sent to user: " + userId);
            } catch (Exception e) {
                System.err.println("[ChatWS] Heartbeat: failed to ping user " + userId + ": " + e.getMessage());
                removeSession(session);
                closeSession(session, CloseStatus.SERVER_ERROR);
            }
        });
    }

    public void sendMessageToUser(String userId, Object message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (Exception e) {
                System.err.println("[ChatWS] Error sending message to user " + userId + ": " + e.getMessage());
                removeSession(session);
            }
        }
    }

    public boolean isUserConnected(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    // ── Presence broadcast ────────────────────────────────────────────────────

    /**
     * Pushes a "presence_update" event to every OTHER user connected on /chat
     * whenever any user goes online or offline.
     */
    private void broadcastPresenceChange(String changedUserId, boolean isOnline) {
        Map<String, Object> presence = userPresenceService.getPresence(changedUserId);
        Map<String, Object> push = new HashMap<>();
        push.put("type", "presence_update");
        push.put("presence", presence);

        int notified = 0;
        for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
            String connectedUserId = entry.getKey();
            WebSocketSession session = entry.getValue();

            if (connectedUserId.equals(changedUserId)) continue;
            if (session == null || !session.isOpen()) continue;

            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(push)));
                notified++;
            } catch (Exception e) {
                System.err.println("[ChatWS] Failed to push presence update to user "
                        + connectedUserId + ": " + e.getMessage());
            }
        }

        System.out.println("[ChatWS] Presence change broadcasted — user: " + changedUserId
                + " is now " + (isOnline ? "ONLINE" : "OFFLINE")
                + " — notified " + notified + " connected user(s) on /chat.");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void removeSession(WebSocketSession session) {
        userSessions.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(session)) {
                userPresenceService.markOffline(entry.getKey(), session.getId()); // pass sessionId
                System.out.println("[ChatWS] Session removed for user: " + entry.getKey());
                return true;
            }
            return false;
        });
        lastPongTime.remove(session.getId());
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        if (session.isOpen()) {
            try {
                session.close(status);
            } catch (Exception ex) {
                System.err.println("[ChatWS] Error closing session " + session.getId() + ": " + ex.getMessage());
            }
        }
    }
}
