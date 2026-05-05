package com.codingworld.service1.service;

import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.model.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);
    private final UserDao userDao;

    public FcmService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Async
    public void sendChatNotificationAsync(User receiver, User sender, String chatId, String timestamp) {
        if (receiver == null || receiver.getFcmToken() == null || receiver.getFcmToken().isBlank()) return;
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized — skipping FCM push");
            return;
        }

        Message msg = Message.builder()
                .setToken(receiver.getFcmToken())
                .setNotification(Notification.builder()
                        .setTitle(sender.getUsername())
                        .setBody("📩 New encrypted message")
                        .build())
                .putData("type", "chat")
                .putData("senderId", sender.getUserId())
                .putData("senderName", sender.getUsername())
                .putData("chatId", chatId == null ? "" : chatId)
                .putData("timestamp", timestamp == null ? "" : timestamp)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId("securechat_messages")
                                .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder().setSound("default").setContentAvailable(true).build())
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(msg);
            log.info("FCM sent to {} -> {}", receiver.getUserId(), response);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            log.warn("FCM error for {}: {} ({})", receiver.getUserId(), e.getMessage(), code);
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                userDao.clearByDeadToken(receiver.getFcmToken());
            }
        } catch (Exception e) {
            log.error("FCM send failed", e);
        }
    }
}
