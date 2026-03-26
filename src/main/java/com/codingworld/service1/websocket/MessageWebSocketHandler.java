package com.codingworld.service1.websocket;

import com.codingworld.service1.model.ChatMessageView;
import com.codingworld.service1.service.ChatMessageService;
import com.codingworld.service1.service.UserPresenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler dedicated to fetching user messages in real time.
 *
 * Endpoint : ws://<host>/ws/messages
 *
 * ── Supported incoming message types ──────────────────────────────────────
 *
 *  1. register   – Must be sent first so the server knows who this socket belongs to.
 *                  { "type": "register", "userId": "42" }
 *
 *  2. get_messages – Fetch conversation history between two users.
 *                  { "type": "get_messages", "userId": "42", "sender": "42", "receiver": "7" }
 *                  • sender   (optional) – one side of the conversation
 *                  • receiver (optional) – other side of the conversation
 *                  • If both sender & receiver are omitted → returns ALL messages for userId.
 *
 *  3. get_user_status – Check the online/offline status of a user.
 *                  { "type": "get_user_status", "userId": "42", "targetUserId": "7" }
 *                  • targetUserId (required) – the user whose status you want to check
 *
 * ── Server responses ───────────────────────────────────────────────────────
 *
 *  Success  : { "type": "messages_response", "status": "ok",    "data": [ ...ChatMessageView... ] }
 *  Error    : { "type": "messages_response", "status": "error", "message": "..." }
 *  Register : { "type": "register_success",  "message": "Registered successfully" }
 *  User Status: { "type": "user_status_response", "presence": { ...user presence data... } }
 *
 * ── Real-time push (triggered by ChatMessageService on send) ───────────────
 *  When a new message arrives for the connected user, the server automatically
 *  pushes:  { "type": "new_message", "data": { ...ChatMessageView... } }
 *  And also pushes presence updates: { "type": "presence_update", "presence": { ...user presence data... } }
 */
@Component
public class MessageWebSocketHandler implements WebSocketHandler {

    // userId -> WebSocketSession  (same pattern as ChatWebSocketHandler)
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private UserPresenceService userPresenceService;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────
    // Connection lifecycle
    // ─────────────────────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("MessageWebSocket connected: " + session.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        String payload = message.getPayload().toString();
        System.out.println("MessageWebSocket received: " + payload);

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

                default:
                    sendError(session, "Unknown message type: " + type);
            }

        } catch (Exception e) {
            System.err.println("MessageWebSocket error: " + e.getMessage());
            sendError(session, "Invalid request: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("MessageWebSocket transport error: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // Find the userId for this session, mark offline, then remove
        userSessions.entrySet().stream()
                .filter(e -> e.getValue().equals(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(userId -> {
                    userPresenceService.markOffline(userId);
                    System.out.println("MessageWebSocket: user " + userId + " marked offline");
                });

        userSessions.entrySet().removeIf(e -> e.getValue().equals(session));
        System.out.println("MessageWebSocket disconnected: " + session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // Handler: register
    // ─────────────────────────────────────────────────────────────

    private void handleRegister(WebSocketSession session, Map<String, Object> data) throws Exception {
        String userId = (String) data.get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            sendError(session, "userId is required for registration");
            return;
        }

        userSessions.put(userId, session);
        userPresenceService.markOnline(userId);
        System.out.println("MessageWebSocket: user " + userId + " registered");

        Map<String, Object> response = new HashMap<>();
        response.put("type", "register_success");
        response.put("message", "Registered successfully");
        send(session, response);
    }

    // ─────────────────────────────────────────────────────────────
    // Handler: get_messages
    // Includes the receiver's online/last_seen status in the response
    // ─────────────────────────────────────────────────────────────

    private void handleGetMessages(WebSocketSession session, Map<String, Object> data) throws Exception {
        String userId   = (String) data.get("userId");
        String sender   = (String) data.get("sender");
        String receiver = (String) data.get("receiver");

        if (userId == null || userId.trim().isEmpty()) {
            sendError(session, "userId is required to fetch messages");
            return;
        }

        List<ChatMessageView> messages = chatMessageService.getMessagesForUser(userId, sender, receiver);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "messages_response");
        response.put("status", "ok");
        response.put("data", messages);

        // Attach the other user's presence status if a specific conversation is requested
        if (receiver != null && !receiver.trim().isEmpty()) {
            response.put("receiverStatus", userPresenceService.getPresence(receiver));
        }
        if (sender != null && !sender.trim().isEmpty() && !sender.equals(userId)) {
            response.put("senderStatus", userPresenceService.getPresence(sender));
        }

        send(session, response);
        System.out.println("MessageWebSocket: sent " + messages.size() + " messages to user " + userId);
    }

    // ─────────────────────────────────────────────────────────────
    // Handler: get_user_status
    // Frontend can ask for any user's online/last_seen at any time
    // ─────────────────────────────────────────────────────────────

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
        System.out.println("MessageWebSocket: sent status of user " + targetUserId);
    }

    // ─────────────────────────────────────────────────────────────
    // Public: push a new message to a connected user
    // Also pushes updated presence of the sender so receiver UI updates
    // ─────────────────────────────────────────────────────────────

    /**
     * Push a newly received message to the target user if they are connected
     * on the /ws/messages socket.
     */
    public void pushNewMessage(String toUserId, ChatMessageView messageView) {
        WebSocketSession session = userSessions.get(toUserId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> push = new HashMap<>();
                push.put("type", "new_message");
                push.put("data", messageView);
                // Include sender's presence so receiver always has fresh online status
                push.put("senderStatus", userPresenceService.getPresence(messageView.getFromUser()));
                send(session, push);
            } catch (Exception e) {
                System.err.println("MessageWebSocket push error for user " + toUserId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Push a presence update to a specific user.
     * Used to notify user A when user B comes online or goes offline.
     */
    public void pushPresenceUpdate(String toUserId, String changedUserId) {
        WebSocketSession session = userSessions.get(toUserId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> push = new HashMap<>();
                push.put("type", "presence_update");
                push.put("presence", userPresenceService.getPresence(changedUserId));
                send(session, push);
            } catch (Exception e) {
                System.err.println("MessageWebSocket presence push error: " + e.getMessage());
            }
        }
    }

    public boolean isUserConnected(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private void send(WebSocketSession session, Object payload) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("type", "messages_response");
            error.put("status", "error");
            error.put("message", errorMessage);
            send(session, error);
        } catch (Exception e) {
            System.err.println("MessageWebSocket could not send error frame: " + e.getMessage());
        }
    }
}

