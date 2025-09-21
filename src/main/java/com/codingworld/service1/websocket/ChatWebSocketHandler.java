package com.codingworld.service1.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    // Store user sessions - userId -> WebSocketSession
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getId());
        // Connection established, but user not logged in yet
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        System.out.println("Received WebSocket message: " + payload);

        try {
            // Parse the message to get user login info
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String type = (String) messageData.get("type");

            if ("login".equals(type)) {
                String userId = (String) messageData.get("userId");
                if (userId != null) {
                    // Store the user session
                    userSessions.put(userId, session);
                    System.out.println("User " + userId + " connected to WebSocket");

                    // Send confirmation to client
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "login_success");
                    response.put("message", "Connected successfully");
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling WebSocket message: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        // Remove user session when connection is closed
        userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Send message to a specific user if they are connected
     */
    public void sendMessageToUser(String userId, Object message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (Exception e) {
                System.err.println("Error sending message to user " + userId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Check if user is connected
     */
    public boolean isUserConnected(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
}
