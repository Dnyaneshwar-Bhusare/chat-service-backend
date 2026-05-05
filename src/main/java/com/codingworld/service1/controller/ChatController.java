package com.codingworld.service1.controller;

import com.codingworld.service1.blockchain.BlockchainService;
import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.model.ChatMessageView;
import com.codingworld.service1.model.Response;
import com.codingworld.service1.model.User;
import com.codingworld.service1.model.dto.ChatMessageRequest;
import com.codingworld.service1.model.dto.VerifyRequest;
import com.codingworld.service1.service.ChatMessageService;
import com.codingworld.service1.service.FcmService;
import com.codingworld.service1.service.UserPresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class ChatController {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private FcmService fcmService;

    @Autowired
    private UserPresenceService userPresenceService;

    @PostMapping("/sendMessage")
    public Response sendMessage(@RequestBody ChatMessageRequest message) {
        System.out.println("Received Message: " + message);

        // Resolve receiver's ETH address from DB
        String receiverEthAddress = null;
        try {
            if (message.getTo() != null && !message.getTo().trim().isEmpty()) {
                User receiver = userDao.getUserById(message.getTo());
                if (receiver != null) {
                    receiverEthAddress = receiver.getEthAddress();
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not resolve receiver ETH address: " + e.getMessage());
        }

        // Store on blockchain
        String txHash = null;
        try {
            txHash = blockchainService.storeMessageHash(message.getMessage(), receiverEthAddress);
            System.out.println("📦 Blockchain tx: " + txHash);
        } catch (Exception e) {
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            if (errMsg.contains("Hash already exists") || errMsg.contains("revert")) {
                System.err.println("⚠️ Blockchain revert (duplicate/expected): " + errMsg);
            } else if (errMsg.contains("invalid JUMP") || errMsg.contains("invalid opcode")
                    || errMsg.contains("Blockchain contract is not initialized")
                    || errMsg.contains("VM Exception")) {
                txHash = blockchainService.handleContractFailure();
                try {
                    blockchainService.storeMessageHash(message.getMessage(), receiverEthAddress);
                    System.out.println("📦 Blockchain tx (after recovery): " + txHash);
                } catch (Exception retryEx) {
                   // System.err.println("⚠️ Retry also failed: " + retryEx.getMessage());
                }
            } else {
                System.err.println("⚠️ Blockchain transaction failed: " + errMsg);
            }
        }

        if (txHash == null) {
            System.out.println("Blockchain server is down or transaction failed.");
            return new Response("0", "Blockchain server is down at the moment, please retry after some time.", null);
        }

        try {
            String chatId = chatMessageService.saveChatMessage(
                    message.getMessageToSelf(),
                    message.getMessage(),
                    message.getFrom(),
                    message.getTo(),
                    message.getAlgo(),
                    txHash
            );
            System.out.println("Chat message saved with ID: " + chatId + " and txHash: " + txHash);

            // Trigger async FCM push to receiver (skip if receiver is online via WebSocket)
            try {
                if (!userPresenceService.isOnline(message.getTo())) {
                    User sender = userDao.getUserByIdWithFcm(message.getFrom());
                    User receiver = userDao.getUserByIdWithFcm(message.getTo());
                    if (sender != null && receiver != null) {
                        fcmService.sendChatNotificationAsync(receiver, sender, chatId, message.getTimestamp());
                    }
                }
            } catch (Exception ignored) { /* never block API on push failure */ }

        } catch (Exception e) {
            System.err.println("❌ Failed to save chat message to DB: " + e.getMessage());
            return new Response("0", "Failed to save message: " + e.getMessage(), null);
        }

        return new Response("1", "ok", "Message sent successfully!");
    }

    @GetMapping("/getUserMessages/{userId}")
    public Response getUserMessages(@PathVariable String userId,
                                    @RequestParam(required = false) String sender,
                                    @RequestParam(required = false) String receiver) {
        try {
            List<ChatMessageView> messages = chatMessageService.getMessagesForUser(userId, sender, receiver);
            return new Response("1", "Messages retrieved successfully", messages);
        } catch (Exception e) {
            System.err.println("Error getting messages for user " + userId + ": " + e.getMessage());
            return new Response("0", "Failed to get messages", null);
        }
    }

    @PostMapping("/verifyMessage")
    public Response verifyMessage(@RequestBody VerifyRequest request) {
        try {
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return new Response("0", "Message content is required for verification", null);
            }
            long timestamp = blockchainService.verifyMessageHash(request.getMessage());
            if (timestamp > 0) {
                return new Response("1", "Message is verified on blockchain", timestamp);
            } else {
                return new Response("0", "Message hash not found on blockchain", null);
            }
        } catch (IllegalStateException e) {
            return new Response("0", "Blockchain not available: " + e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("Blockchain verification error: " + e.getMessage());
            return new Response("0", "Verification failed: " + e.getMessage(), null);
        }
    }

    @GetMapping("/verifyMessage/{chatId}")
    public Response verifyMessageByChatId(@PathVariable String chatId) {
        ChatMessageView msg;
        try {
            msg = chatMessageService.getMessageByChatId(chatId);
        } catch (Exception e) {
            System.err.println("DB lookup failed for chatId " + chatId + ": " + e.getMessage());
            return new Response("0", "Failed to fetch message: " + e.getMessage(), null);
        }

        if (msg == null) {
            return new Response("0", "No message found with chatId: " + chatId, null);
        }
        if (msg.getTxHash() == null || msg.getTxHash().isBlank()) {
            return new Response("0", "Message was not anchored to blockchain (no tx_hash)", null);
        }

        try {
            long timestamp = blockchainService.verifyMessageHash(msg.getMessage());
            if (timestamp > 0) {
                return new Response("1", "Message is verified on blockchain", timestamp);
            } else {
                return new Response("0", "Message hash not found on blockchain", null);
            }
        } catch (Exception e) {
            System.err.println("Blockchain verification error for chatId " + chatId + ": " + e.getMessage());
            return new Response("0", "Verification failed: " + e.getMessage(), null);
        }
    }

    /**
     * DELETE /deleteMessage/{chatId}?userId={userId}
     * Deletes a single message. Only the original sender can delete it.
     */
    @DeleteMapping("/deleteMessage/{chatId}")
    public Response deleteMessage(@PathVariable String chatId) {
        try {
            if (chatId == null || chatId.trim().isEmpty()) {
                return new Response("0", "chatId is required", null);
            }

            boolean deleted = chatMessageService.deleteMessageById(chatId);
            if (deleted) {
                return new Response("1", "Message deleted successfully", null);
            } else {
                return new Response("0", "Message not found", null);
            }
        } catch (Exception e) {
            System.err.println("Error deleting message " + chatId + ": " + e.getMessage());
            return new Response("0", "Failed to delete message: " + e.getMessage(), null);
        }
    }

    /**
     * DELETE /deleteConversation?userId1={userId1}&userId2={userId2}
     * Deletes all messages exchanged between two users (both directions).
     */
    @DeleteMapping("/deleteConversation")
    public Response deleteConversation(@RequestParam String userId1,
                                       @RequestParam String userId2) {
        try {
            if (userId1 == null || userId1.trim().isEmpty() ||
                userId2 == null || userId2.trim().isEmpty()) {
                return new Response("0", "Both userId1 and userId2 are required", null);
            }

            int count = chatMessageService.deleteConversation(userId1, userId2);
            return new Response("1", "Conversation deleted successfully", count + " message(s) deleted");
        } catch (Exception e) {
            System.err.println("Error deleting conversation between " + userId1 + " and " + userId2 + ": " + e.getMessage());
            return new Response("0", "Failed to delete conversation: " + e.getMessage(), null);
        }
    }

    /**
     * DELETE /deleteAllMessages/{userId}
     * Deletes ALL messages sent or received by a user.
     */
    @DeleteMapping("/deleteAllMessages/{userId}")
    public Response deleteAllMessages(@PathVariable String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return new Response("0", "userId is required", null);
            }

            int count = chatMessageService.deleteAllMessagesForUser(userId);
            return new Response("1", "All messages deleted successfully", count + " message(s) deleted");
        } catch (Exception e) {
            System.err.println("Error deleting all messages for user " + userId + ": " + e.getMessage());
            return new Response("0", "Failed to delete messages: " + e.getMessage(), null);
        }
    }
}
