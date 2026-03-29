package com.codingworld.service1.service;

import com.codingworld.service1.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

@Service
public class UserPresenceService {

    // userId → set of active sessionIds (a user can be connected on both /chat and /ws/messages)
    private final Map<String, Set<String>> onlineUserSessions = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserDao userDao;

    private final List<BiConsumer<String, Boolean>> presenceChangeListeners =
            new CopyOnWriteArrayList<>();

    public void addPresenceChangeListener(BiConsumer<String, Boolean> listener) {
        presenceChangeListeners.add(listener);
        System.out.println("[Presence] Listener registered, total listeners: " + presenceChangeListeners.size());
    }

    /**
     * Mark a specific session of a user as online.
     * Only broadcasts "online" on the FIRST session (i.e., user was truly offline before).
     */
    public void markOnline(String userId, String sessionId) {
        boolean wasOffline = !onlineUserSessions.containsKey(userId)
                || onlineUserSessions.get(userId).isEmpty();

        onlineUserSessions
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

        System.out.println("[Presence] User " + userId + " session " + sessionId
                + " marked online. Total sessions: " + onlineUserSessions.get(userId).size());

        if (wasOffline) {
            System.out.println("[Presence] User " + userId + " is now ONLINE");
            notifyPresenceChange(userId, true);
        }
    }

    /**
     * Mark a specific session of a user as offline.
     * Only broadcasts "offline" when the LAST session disconnects.
     */
    public void markOffline(String userId, String sessionId) {
        Set<String> sessions = onlineUserSessions.get(userId);
        if (sessions == null) return;

        sessions.remove(sessionId);
        System.out.println("[Presence] User " + userId + " session " + sessionId
                + " removed. Remaining sessions: " + sessions.size());

        if (sessions.isEmpty()) {
            onlineUserSessions.remove(userId);
            LocalDateTime now = LocalDateTime.now();
            try {
                userDao.updateLastSeen(userId, now);
            } catch (Exception e) {
                System.err.println("[Presence] Failed to update last_seen for user " + userId + ": " + e.getMessage());
            }
            System.out.println("[Presence] User " + userId + " is now OFFLINE — last seen: " + now.format(FORMATTER));
            notifyPresenceChange(userId, false);
        }
    }

    private void notifyPresenceChange(String userId, boolean isOnline) {
        for (BiConsumer<String, Boolean> listener : presenceChangeListeners) {
            try {
                listener.accept(userId, isOnline);
            } catch (Exception e) {
                System.err.println("[Presence] Listener error for user " + userId + ": " + e.getMessage());
            }
        }
    }

    public boolean isOnline(String userId) {
        Set<String> sessions = onlineUserSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public String getLastSeen(String userId) {
        if (isOnline(userId)) return null;
        try {
            LocalDateTime lastSeen = userDao.getLastSeen(userId);
            if (lastSeen == null) return null;
            return lastSeen.format(FORMATTER);
        } catch (Exception e) {
            System.err.println("[Presence] Failed to fetch last_seen for user " + userId + ": " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getPresence(String userId) {
        boolean online = isOnline(userId);
        // Use HashMap (not ConcurrentHashMap) — ConcurrentHashMap does NOT allow null values
        Map<String, Object> presence = new java.util.HashMap<>();
        presence.put("userId",   userId);
        presence.put("isOnline", online);
        presence.put("lastSeen", online ? null : getLastSeen(userId));
        return presence;
    }
}
