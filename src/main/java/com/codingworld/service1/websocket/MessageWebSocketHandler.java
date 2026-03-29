package com.codingworld.service1.websocket;

import com.codingworld.service1.model.ChatMessageView;
import com.codingworld.service1.service.ChatMessageService;
import com.codingworld.service1.service.UserPresenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageWebSocketHandler implements WebSocketHandler {

    private final Map<String, WebSocketSession> userSessions      = new ConcurrentHashMap<>();
    private final Map<String, Long>             lastPongTime       = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime>    lastDisconnectTime = new ConcurrentHashMap<>();

    /**
     * presenceWatchers: receiverId → Set of watcherUserIds
     * When user 17 opens chat with user 20, we store: "20" → {"17"}
     * So when user 20 comes online/offline, we immediately push to user 17.
     */
    private final Map<String, Set<String>> presenceWatchers = new ConcurrentHashMap<>();

    private static final long HEARTBEAT_INTERVAL_MS = 30_000;
    private static final long PONG_TIMEOUT_MS        = 60_000;

    @Autowired private ChatMessageService  chatMessageService;
    @Autowired private UserPresenceService userPresenceService;
    @Autowired private ObjectMapper        objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        lastPongTime.put(session.getId(), System.currentTimeMillis());
        System.out.println("[MessageWS] Connection established: " + session.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // Handle WebSocket-level pong frame
        if (message instanceof PongMessage) {
            lastPongTime.put(session.getId(), System.currentTimeMillis());
            System.out.println("[MessageWS] Pong received from session: " + session.getId());
            return;
        }

        String payload = message.getPayload().toString();
        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");

            switch (type != null ? type : "") {
                case "register":
                    handleRegister(session, data);
                    break;
                case "get_messages":
                    handleGetMessages(session, data);
                    break;
                case "get_user_status":
                    handleGetUserStatus(session, data);
                    break;
                case "ping":
                    // application-level ping — reply with pong
                    lastPongTime.put(session.getId(), System.currentTimeMillis());
                    Map<String, Object> pong = new HashMap<>();
                    pong.put("type", "pong");
                    send(session, pong);
                    break;
                default:
                    sendError(session, "Unknown message type: " + type);
            }

        } catch (Exception e) {
            System.err.println("[MessageWS] Error handling message: " + e.getMessage());
            sendError(session, "Invalid request: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String cause = exception.getMessage();
        if (cause != null && cause.contains("Connection reset by peer")) {
            System.out.println("[MessageWS] Client disconnected abruptly (Connection reset by peer) on session: " + session.getId());
        } else {
            System.err.println("[MessageWS] Transport error on session " + session.getId() + ": " + cause);
        }
        // Session is already broken — only clean up, do NOT try to close it
        removeSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("[MessageWS] Connection closed: " + session.getId() + " | status: " + status);
        removeSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // ── Heartbeat ─────────────────────────────────────────────────────────────

    /**
     * Runs every 30 seconds. Sends a WebSocket ping to every active session.
     * Any session that has not responded with a pong within PONG_TIMEOUT_MS
     * is considered stale and closed so the client can reconnect cleanly.
     */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeat() {
        long now = System.currentTimeMillis();
        userSessions.forEach((userId, session) -> {
            if (!session.isOpen()) {
                System.out.println("[MessageWS] Heartbeat: session already closed for user " + userId + ", cleaning up.");
                removeSession(session);
                return;
            }

            long lastPong = lastPongTime.getOrDefault(session.getId(), now);
            if (now - lastPong > PONG_TIMEOUT_MS) {
                System.err.println("[MessageWS] Heartbeat: no pong from user " + userId
                        + " for " + ((now - lastPong) / 1000) + "s — closing stale session.");
                removeSession(session);
                closeSession(session, CloseStatus.SESSION_NOT_RELIABLE);
                return;
            }

            try {
                session.sendMessage(new PingMessage());
                System.out.println("[MessageWS] Heartbeat ping sent to user: " + userId);
            } catch (Exception e) {
                System.err.println("[MessageWS] Heartbeat: failed to ping user " + userId + ": " + e.getMessage());
                removeSession(session);
                closeSession(session, CloseStatus.SERVER_ERROR);
            }
        });
    }

    // ── Message handlers ──────────────────────────────────────────────────────

    private void handleRegister(WebSocketSession session, Map<String, Object> data) throws Exception {
        String userId = (String) data.get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            sendError(session, "userId is required for registration");
            return;
        }

        WebSocketSession oldSession = userSessions.get(userId);
        if (oldSession != null && !oldSession.getId().equals(session.getId())) {
            System.out.println("[MessageWS] Replacing stale session for user: " + userId);
            closeSession(oldSession, CloseStatus.SESSION_NOT_RELIABLE);
        }

        userSessions.put(userId, session);
        userPresenceService.markOnline(userId, session.getId()); // pass sessionId

        Map<String, Object> response = new HashMap<>();
        response.put("type", "register_success");
        response.put("message", "Registered successfully");
        send(session, response);
        System.out.println("[MessageWS] User registered: " + userId);

        // Deliver any messages missed while the client was disconnected
        deliverMissedMessages(userId, session);
    }

    private void handleGetMessages(WebSocketSession session, Map<String, Object> data) throws Exception {
        String userId   = (String) data.get("userId");
        String sender   = (String) data.get("sender");
        String receiver = (String) data.get("receiver");

        if (userId == null || userId.trim().isEmpty()) {
            sendError(session, "userId is required to fetch messages");
            return;
        }

        // Track presence watch: userId is now watching receiver's status
        if (receiver != null && !receiver.trim().isEmpty()) {
            presenceWatchers
                .computeIfAbsent(receiver, k -> ConcurrentHashMap.newKeySet())
                .add(userId);
            System.out.println("[MessageWS] User " + userId + " is now watching presence of user " + receiver);
        }

        List<ChatMessageView> messages = chatMessageService.getMessagesForUser(userId, sender, receiver);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "messages_response");
        response.put("status", "ok");
        response.put("data", messages);

        if (receiver != null && !receiver.trim().isEmpty()) {
            // Always send the LIVE current status at request time
            response.put("receiverStatus", userPresenceService.getPresence(receiver));
        }
        if (sender != null && !sender.trim().isEmpty() && !sender.equals(userId)) {
            response.put("senderStatus", userPresenceService.getPresence(sender));
        }

        send(session, response);
    }

    private void handleGetUserStatus(WebSocketSession session, Map<String, Object> data) throws Exception {
        String targetUserId = (String) data.get("targetUserId");
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            sendError(session, "targetUserId is required for get_user_status");
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "user_status_response");
        response.put("presence", userPresenceService.getPresence(targetUserId));
        send(session, response);
    }

    // ── Missed messages on reconnect ──────────────────────────────────────────

    /**
     * After a client re-registers (reconnects), fetch all messages that arrived
     * while they were offline and push them as a "missed_messages" event.
     */
    private void deliverMissedMessages(String userId, WebSocketSession session) {
        LocalDateTime disconnectedAt = lastDisconnectTime.get(userId);
        if (disconnectedAt == null) {
            // First-ever connection — nothing to catch up on
            return;
        }

        try {
            List<ChatMessageView> missed = chatMessageService.getNewMessagesForUser(userId, disconnectedAt);
            if (missed == null || missed.isEmpty()) {
                System.out.println("[MessageWS] No missed messages for user: " + userId);
                return;
            }

            Map<String, Object> push = new HashMap<>();
            push.put("type", "missed_messages");
            push.put("count", missed.size());
            push.put("data", missed);
            send(session, push);
            System.out.println("[MessageWS] Delivered " + missed.size() + " missed message(s) to user: " + userId);
        } catch (Exception e) {
            System.err.println("[MessageWS] Failed to deliver missed messages to " + userId + ": " + e.getMessage());
        }
    }

    // ── Push helpers ──────────────────────────────────────────────────────────

    public void pushNewMessage(String toUserId, ChatMessageView messageView) {
        WebSocketSession session = userSessions.get(toUserId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> push = new HashMap<>();
                push.put("type", "new_message");
                push.put("data", messageView);
                push.put("senderStatus", userPresenceService.getPresence(messageView.getFromUser()));
                send(session, push);
            } catch (Exception e) {
                System.err.println("[MessageWS] Push error for user " + toUserId + ": " + e.getMessage());
                removeSession(session);
            }
        }
    }

    public void pushPresenceUpdate(String toUserId, String changedUserId) {
        WebSocketSession session = userSessions.get(toUserId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> push = new HashMap<>();
                push.put("type", "presence_update");
                push.put("presence", userPresenceService.getPresence(changedUserId));
                send(session, push);
            } catch (Exception e) {
                System.err.println("[MessageWS] Presence push error: " + e.getMessage());
                removeSession(session);
            }
        }
    }

    public boolean isUserConnected(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void removeSession(WebSocketSession session) {
        userSessions.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(session)) {
                String userId = entry.getKey();
                userPresenceService.markOffline(userId, session.getId()); // pass sessionId
                lastDisconnectTime.put(userId, LocalDateTime.now());
                // Remove this user from all watcher sets they were in
                presenceWatchers.values().forEach(watchers -> watchers.remove(userId));
                System.out.println("[MessageWS] Session removed, disconnect time recorded for user: " + userId);
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
                System.err.println("[MessageWS] Error closing session " + session.getId() + ": " + ex.getMessage());
            }
        }
    }

    private void send(WebSocketSession session, Object payload) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("type", "error");
            error.put("message", errorMessage);
            send(session, error);
        } catch (Exception e) {
            System.err.println("[MessageWS] Failed to send error message: " + e.getMessage());
        }
    }

    // ── Presence broadcast ────────────────────────────────────────────────────

    /**
     * Called by UserPresenceService whenever any user's presence changes.
     *
     * Two-pass broadcast:
     *  1. Push to ALL connected users (generic contacts list update)
     *  2. Push an EXTRA targeted update to any user who is actively
     *     watching this user (e.g., has their chat open) — this fixes
     *     the case where receiverStatus is stale after a get_messages call.
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
                send(session, push);
                notified++;
            } catch (Exception e) {
                System.err.println("[MessageWS] Failed to push presence update to user "
                        + connectedUserId + ": " + e.getMessage());
            }
        }

        // Extra targeted push to watchers (users actively viewing a chat with changedUserId)
        Set<String> watchers = presenceWatchers.get(changedUserId);
        if (watchers != null && !watchers.isEmpty()) {
            Map<String, Object> watcherPush = new HashMap<>();
            watcherPush.put("type", "receiver_status_update");
            watcherPush.put("presence", presence);

            for (String watcherUserId : watchers) {
                WebSocketSession watcherSession = userSessions.get(watcherUserId);
                if (watcherSession == null || !watcherSession.isOpen()) continue;
                try {
                    send(watcherSession, watcherPush);
                    System.out.println("[MessageWS] receiver_status_update sent to watcher "
                            + watcherUserId + " about user " + changedUserId
                            + " — isOnline=" + isOnline);
                } catch (Exception e) {
                    System.err.println("[MessageWS] Failed to send receiver_status_update to "
                            + watcherUserId + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[MessageWS] Presence broadcasted — user: " + changedUserId
                + " isOnline=" + isOnline
                + " — notified " + notified + " user(s), watchers=" + (watchers != null ? watchers.size() : 0));
    }

    @PostConstruct
    public void init() {
        userPresenceService.addPresenceChangeListener(this::broadcastPresenceChange);
        System.out.println("[MessageWS] Presence change listener registered.");
    }
}
