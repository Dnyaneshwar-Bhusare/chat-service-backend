package com.codingworld.service1.service;

import com.codingworld.service1.dao.ChatMessageDao;
import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.model.ChatMessageEntity;
import com.codingworld.service1.model.ChatMessageView;
import com.codingworld.service1.websocket.ChatWebSocketHandler;
import com.codingworld.service1.websocket.MessageWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageDao chatMessageDao;

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @Autowired
    @Lazy
    private MessageWebSocketHandler messageWebSocketHandler;

    @Autowired
    private UserDao userDao;

    public String saveChatMessage(String messageToSelf, String message, String fromUser, String toUser, String algo, String txHash) {
        try {
            ChatMessageEntity chatMessage = new ChatMessageEntity();
            chatMessage.setMessageToSelf(messageToSelf);
            chatMessage.setMessage(message);
            chatMessage.setFromUser(fromUser);
            chatMessage.setToUser(toUser);
            chatMessage.setAlgo(algo);
            chatMessage.setTxHash(txHash);
            chatMessage.setCreatedTs(LocalDateTime.now());

            String chatId = chatMessageDao.saveChatMessage(chatMessage);

            String publicKey = null;
            try {
                publicKey = userDao.getUserByUsername(fromUser).getPublicKey();
            } catch (Exception ex) {
            }

            ChatMessageView messageView = new ChatMessageView(
                    chatId, message, fromUser, toUser, algo, chatMessage.getCreatedTs(), txHash, publicKey
            );

            if (webSocketHandler.isUserConnected(toUser)) {
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", "new_message");
                notificationData.put("data", messageView);
                webSocketHandler.sendMessageToUser(toUser, notificationData);
            }

            messageWebSocketHandler.pushNewMessage(toUser, messageView);

            return chatId;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save chat message: " + e.getMessage(), e);
        }
    }

    public List<ChatMessageView> getMessagesForUser(String userId, String sender, String reciver) {
        try {
            return chatMessageDao.getMessagesForUser(userId, sender, reciver);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get messages for user: " + e.getMessage(), e);
        }
    }

    public List<ChatMessageView> getNewMessagesForUser(String userId, LocalDateTime afterTime) {
        try {
            return chatMessageDao.getNewMessagesForUser(userId, afterTime);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get new messages: " + e.getMessage(), e);
        }
    }

    public ChatMessageView getMessageByChatId(String chatId) {
        try {
            return chatMessageDao.getMessageByChatId(chatId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get message by chatId: " + e.getMessage(), e);
        }
    }

    public boolean deleteMessageById(String chatId) {
        try {
            return chatMessageDao.deleteMessageById(chatId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete message: " + e.getMessage(), e);
        }
    }

    public int deleteConversation(String userId1, String userId2) {
        try {
            return chatMessageDao.deleteConversation(userId1, userId2);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete conversation: " + e.getMessage(), e);
        }
    }

    public int deleteAllMessagesForUser(String userId) {
        try {
            return chatMessageDao.deleteAllMessagesForUser(userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all messages for user: " + e.getMessage(), e);
        }
    }
}
