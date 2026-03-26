package com.codingworld.service1.service;

import com.codingworld.service1.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserPresenceService {

    private final Map<String, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserDao userDao;

    public void markOnline(String userId) {
        onlineUsers.put(userId, LocalDateTime.now());
        System.out.println("User " + userId + " is now ONLINE");
    }

    public void markOffline(String userId) {
        LocalDateTime now = LocalDateTime.now();
        onlineUsers.remove(userId);
        try {
            userDao.updateLastSeen(userId, now);
        } catch (Exception e) {
            System.err.println("Failed to update last_seen for user " + userId + ": " + e.getMessage());
        }
        System.out.println("User " + userId + " is now OFFLINE — last seen: " + now.format(FORMATTER));
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
            System.err.println("Failed to fetch last_seen for user " + userId + ": " + e.getMessage());
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
