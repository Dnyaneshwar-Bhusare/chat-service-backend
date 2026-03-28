package com.codingworld.service1.service;

import com.codingworld.service1.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Service
public class UserPresenceService {

    private final Map<String, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserDao userDao;

    /**
     * Callback registered by MessageWebSocketHandler.
     * Called whenever any user's presence changes (online/offline).
     * BiConsumer<changedUserId, isOnline>
     */
    private BiConsumer<String, Boolean> presenceChangeListener;

    public void setPresenceChangeListener(BiConsumer<String, Boolean> listener) {
        this.presenceChangeListener = listener;
    }

    public void markOnline(String userId) {
        onlineUsers.put(userId, LocalDateTime.now());
        System.out.println("[Presence] User " + userId + " is now ONLINE");
        notifyPresenceChange(userId, true);
    }

    public void markOffline(String userId) {
        LocalDateTime now = LocalDateTime.now();
        onlineUsers.remove(userId);
        try {
            userDao.updateLastSeen(userId, now);
        } catch (Exception e) {
            System.err.println("[Presence] Failed to update last_seen for user " + userId + ": " + e.getMessage());
        }
        System.out.println("[Presence] User " + userId + " is now OFFLINE — last seen: " + now.format(FORMATTER));
        notifyPresenceChange(userId, false);
    }

    private void notifyPresenceChange(String userId, boolean isOnline) {
        if (presenceChangeListener != null) {
            try {
                presenceChangeListener.accept(userId, isOnline);
            } catch (Exception e) {
                System.err.println("[Presence] Failed to notify presence change for user " + userId + ": " + e.getMessage());
            }
        }
    }

    public boolean isOnline(String userId) {
        return onlineUsers.containsKey(userId);
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
        Map<String, Object> presence = new ConcurrentHashMap<>();
        presence.put("userId", userId);
        presence.put("isOnline", online);
        presence.put("lastSeen", online ? null : getLastSeen(userId));
        return presence;
    }
}
