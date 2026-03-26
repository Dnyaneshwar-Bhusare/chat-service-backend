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

@Component
public class MessageWebSocketHandler implements WebSocketHandler {

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private UserPresenceService userPresenceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("MessageWebSocket connected: " + session.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
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

    private void handleRegister(WebSocketSession session, Map<String, Object> data) throws Exception {
        String userId = (String) data.get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            sendError(session, "userId is required for registration");
            return;
        }

        userSessions.put(userId, session);
        userPresenceService.markOnline(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "register_success");
        response.put("message", "Registered successfully");
        send(session, response);
    }

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

        if (receiver != null && !receiver.trim().isEmpty()) {
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
                System.err.println("MessageWebSocket push error for user " + toUserId + ": " + e.getMessage());
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
                System.err.println("MessageWebSocket presence push error: " + e.getMessage());
            }
        }
    }

    public boolean isUserConnected(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

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
