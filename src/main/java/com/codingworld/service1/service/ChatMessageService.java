package com.codingworld.service1.service;

import com.codingworld.service1.dao.ChatMessageDao;
import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.model.ChatMessageEntity;
import com.codingworld.service1.model.ChatMessageView;
import com.codingworld.service1.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer for Chat Message operations
 */
@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageDao chatMessageDao;

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @Autowired
    private UserDao userDao;

    /**
     * Save chat message with transaction hash to database and send real-time notification
     * @param message The actual message content
     * @param fromUser Sender username/email
     * @param toUser Receiver username/email
     * @param algo Encryption algorithm used
     * @param txHash Blockchain transaction hash
     * @return Generated chat_id
     */
    public String saveChatMessage(String messageToSelf,String message, String fromUser, String toUser, String algo, String txHash) {
        try {
            // Create chat message entity
            ChatMessageEntity chatMessage = new ChatMessageEntity();
            chatMessage.setMessageToSelf(messageToSelf);
            chatMessage.setMessage(message);
            chatMessage.setFromUser(fromUser);
            chatMessage.setToUser(toUser);
            chatMessage.setAlgo(algo);
            chatMessage.setTxHash(txHash);
            chatMessage.setCreatedTs(LocalDateTime.now());

            // Save to database and return generated chat_id
            String chatId = chatMessageDao.saveChatMessage(chatMessage);

            // Fetch sender's public key
            String publicKey = null;
            try {
                publicKey = userDao.getUserByUsername(fromUser).getPublicKey();
            } catch (Exception ex) {
                // If user not found or error, leave publicKey as null
            }

            // Send real-time notification to recipient if connected
            if (webSocketHandler.isUserConnected(toUser)) {
                ChatMessageView messageView = new ChatMessageView(
                    chatId, message, fromUser, toUser, algo, chatMessage.getCreatedTs(), txHash, publicKey
                );

                // Create map using Java 8 compatible approach
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", "new_message");
                notificationData.put("data", messageView);

                webSocketHandler.sendMessageToUser(toUser, notificationData);
            }

            return chatId;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save chat message: " + e.getMessage(), e);
        }
    }

    /**
     * Get all messages for a specific user
     * @param userId User ID to get messages for
     * @return List of chat messages
     */
    public List<ChatMessageView> getMessagesForUser(String userId,String sender, String reciver) {
        try {
            return chatMessageDao.getMessagesForUser(userId,sender, reciver);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get messages for user: " + e.getMessage(), e);
        }
    }

    /**
     * Get new messages for a user after specific time
     * @param userId User ID
     * @param afterTime Get messages after this time
     * @return List of new messages
     */
    public List<ChatMessageView> getNewMessagesForUser(String userId, LocalDateTime afterTime) {
        try {
            return chatMessageDao.getNewMessagesForUser(userId, afterTime);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get new messages: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch a single message by its chat_id.
     * Returns null if not found.
     */
    public ChatMessageView getMessageByChatId(String chatId) {
        try {
            return chatMessageDao.getMessageByChatId(chatId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get message by chatId: " + e.getMessage(), e);
        }
    }
}
