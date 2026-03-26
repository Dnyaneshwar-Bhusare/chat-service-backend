package com.codingworld.service1.service;

import com.codingworld.service1.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single source of truth for user online/offline status and last seen.
 *
 * Online status  — stored in memory (ConcurrentHashMap). No DB needed.
 *                  Accurate as long as the server is running.
 *
 * Last seen      — stored in DB (users.last_seen column) on every disconnect.
 *                  Survives server restarts.
 */
@Service
public class UserPresenceService {

    // userId -> time they came online (in-memory only)
    private final Map<String, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserDao userDao;

    // ─────────────────────────────────────────────────────────────
    // Called by WebSocket handlers on connect / disconnect
    // ─────────────────────────────────────────────────────────────

    /**
     * Mark a user as online.
     * Call this when the user registers on any WebSocket.
     */
    public void markOnline(String userId) {
        onlineUsers.put(userId, LocalDateTime.now());
        System.out.println("👤 User " + userId + " is now ONLINE");
    }

    /**
     * Mark a user as offline and persist last_seen to DB.
     * Call this when the user's WebSocket disconnects.
     */
    public void markOffline(String userId) {
        LocalDateTime now = LocalDateTime.now();
        onlineUsers.remove(userId);
        // Persist to DB so last_seen survives server restarts
        try {
            userDao.updateLastSeen(userId, now);
        } catch (Exception e) {
            System.err.println("Failed to update last_seen for user " + userId + ": " + e.getMessage());
        }
        System.out.println("👤 User " + userId + " is now OFFLINE — last seen: " + now.format(FORMATTER));
    }

    // ─────────────────────────────────────────────────────────────
    // Status queries
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns true if the user currently has an active WebSocket connection.
     */
    public boolean isOnline(String userId) {
        return onlineUsers.containsKey(userId);
    }

    /**
     * Returns the last seen timestamp as a formatted string.
     * - If online  → returns null (frontend should show "Online" instead)
     * - If offline → returns DB value (persisted on last disconnect)
     */
    public String getLastSeen(String userId) {
        if (isOnline(userId)) return null;
        try {
            LocalDateTime lastSeen = userDao.getLastSeen(userId);
            if (lastSeen == null) return null;
            return lastSeen.format(FORMATTER);
        } catch (Exception e) {
            System.err.println("Failed to fetch last_seen for user " + userId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns a full presence status map for a given userId.
     * Used to embed status directly in API/WebSocket responses.
     *
     * Shape:
     * {
     *   "userId":   "7",
     *   "isOnline": true,
     *   "lastSeen": null          ← null when online
     * }
     * or
     * {
     *   "userId":   "7",
     *   "isOnline": false,
     *   "lastSeen": "2026-03-26 10:45:00"
     * }
     */
    public Map<String, Object> getPresence(String userId) {
        boolean online = isOnline(userId);
        Map<String, Object> presence = new ConcurrentHashMap<>();
        presence.put("userId",   userId);
        presence.put("isOnline", online);
        presence.put("lastSeen", online ? null : getLastSeen(userId));
        return presence;
    }
}

