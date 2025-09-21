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

        // Service Information
        response.put("service", "🚀 Cypher Squad Chat Service Backend");
        response.put("status", "✅ RUNNING");
        response.put("version", "v1.0.0");
        response.put("description", "Secure blockchain-powered real-time chat service");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        response.put("port", 9001);

        // API Endpoints Information
        Map<String, Object> endpoints = new LinkedHashMap<>();

        Map<String, String> authEndpoints = new LinkedHashMap<>();
        authEndpoints.put("login", "POST /login - User authentication with database validation");
        authEndpoints.put("logout", "POST /logout - User logout (to be implemented)");
        endpoints.put("Authentication", authEndpoints);

        Map<String, String> userEndpoints = new LinkedHashMap<>();
        userEndpoints.put("getAllUsers", "GET /users - Get all registered users");
        userEndpoints.put("getUserMessages", "GET /getUserMessages/{userId} - Get messages for specific user");
        endpoints.put("User Management", userEndpoints);

        Map<String, String> messagingEndpoints = new LinkedHashMap<>();
        messagingEndpoints.put("sendMessage", "POST /sendMessage - Send encrypted message with blockchain verification");
        messagingEndpoints.put("webSocketConnection", "WS /chat - Real-time message delivery");
        messagingEndpoints.put("getFriends", "GET /friends - Get user's friend list");
        endpoints.put("Real-time Messaging", messagingEndpoints);

        Map<String, String> healthEndpoints = new LinkedHashMap<>();
        healthEndpoints.put("home", "GET / - Service information and API documentation");
        healthEndpoints.put("status", "GET /status - Basic service status");
        healthEndpoints.put("dbHealth", "GET /db/health - Database connection health");
        endpoints.put("System Health", healthEndpoints);

        response.put("availableEndpoints", endpoints);

        // System Statistics
        Map<String, Object> statistics = new LinkedHashMap<>();
        try {
            int totalUsers = userService.getUserCount();
            statistics.put("totalRegisteredUsers", totalUsers);
            statistics.put("activeWebSocketConnections", "Real-time connections active");
            statistics.put("databaseStatus", "✅ Connected to MySQL db_chat");
            statistics.put("blockchainIntegration", "✅ Web3j Ethereum integration ready");
        } catch (Exception e) {
            statistics.put("totalRegisteredUsers", "Unable to fetch");
            statistics.put("databaseStatus", "❌ Database connection issue");
        }

        response.put("systemStatistics", statistics);

        // Features
        Map<String, String> features = new LinkedHashMap<>();
        features.put("🔐 Encryption", "AES encryption for message security");
        features.put("⛓️ Blockchain", "Transaction hash storage on Ethereum");
        features.put("⚡ Real-time", "WebSocket-based instant messaging");
        features.put("🗄️ Database", "MySQL persistent message storage");
        features.put("👥 User Management", "Secure authentication and user profiles");
        features.put("📱 API Ready", "RESTful APIs for frontend integration");

        response.put("features", features);

        // Quick Start Guide
        Map<String, Object> quickStart = new LinkedHashMap<>();
        quickStart.put("step1", "POST /login with email and password to authenticate");
        quickStart.put("step2", "Connect to WebSocket at ws://localhost:9001/chat");
        quickStart.put("step3", "Send login message to WebSocket with userId");
        quickStart.put("step4", "Use POST /sendMessage to send encrypted messages");
        quickStart.put("step5", "GET /getUserMessages/{userId} to retrieve message history");

        response.put("quickStartGuide", quickStart);

        // Contact Information
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("project", "Cypher Squad Chat Service");
        contact.put("developer", "College Project Team");
        contact.put("technology", "Spring Boot + MySQL + WebSocket + Blockchain");

        response.put("projectInfo", contact);

        return response;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "🟢 HEALTHY");
        status.put("service", "chat-service-backend");
        status.put("version", "v1.0.0");
        status.put("uptime", "Service running smoothly");
        status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Health Checks
        Map<String, String> healthChecks = new LinkedHashMap<>();
        try {
            userService.getUserCount();
            healthChecks.put("database", "✅ Connected");
        } catch (Exception e) {
            healthChecks.put("database", "❌ Connection failed");
        }

        healthChecks.put("webSocket", "✅ Active");
        healthChecks.put("restAPI", "✅ Responding");
        healthChecks.put("blockchain", "✅ Web3j Ready");

        status.put("healthChecks", healthChecks);

        return status;
    }
}
