package com.codingworld.service1.controller;

import com.codingworld.service1.service.UserService;
import com.codingworld.service1.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "Cypher Squad Chat Service Backend");
        response.put("status", "RUNNING");
        response.put("version", "v1.0.0");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        response.put("port", 9001);

        Map<String, Object> endpoints = new LinkedHashMap<>();

        Map<String, String> authEndpoints = new LinkedHashMap<>();
        authEndpoints.put("login", "POST /login");
        authEndpoints.put("signUp", "POST /signUp");
        authEndpoints.put("updateProfilePic", "POST /updateProfilePic");
        endpoints.put("Authentication", authEndpoints);

        Map<String, String> userEndpoints = new LinkedHashMap<>();
        userEndpoints.put("getAllUsers", "GET /users");
        userEndpoints.put("getUserMessages", "GET /getUserMessages/{userId}");
        endpoints.put("User Management", userEndpoints);

        Map<String, String> messagingEndpoints = new LinkedHashMap<>();
        messagingEndpoints.put("sendMessage", "POST /sendMessage");
        messagingEndpoints.put("webSocket", "WS /chat");
        messagingEndpoints.put("wsMessages", "WS /ws/messages");
        endpoints.put("Messaging", messagingEndpoints);

        response.put("availableEndpoints", endpoints);

        Map<String, Object> statistics = new LinkedHashMap<>();
        try {
            statistics.put("totalRegisteredUsers", userService.getUserCount());
            statistics.put("databaseStatus", "Connected");
        } catch (Exception e) {
            statistics.put("totalRegisteredUsers", "N/A");
            statistics.put("databaseStatus", "Connection issue");
        }
        response.put("systemStatistics", statistics);

        return response;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "HEALTHY");
        status.put("service", "chat-service-backend");
        status.put("version", "v1.0.0");
        status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        Map<String, String> healthChecks = new LinkedHashMap<>();
        try {
            userService.getUserCount();
            healthChecks.put("database", "Connected");
        } catch (Exception e) {
            healthChecks.put("database", "Connection failed");
        }
        healthChecks.put("webSocket", "Active");
        healthChecks.put("restAPI", "Responding");

        status.put("healthChecks", healthChecks);
        return status;
    }
}
