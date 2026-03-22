package com.codingworld.service1.controller;

import com.codingworld.service1.blockchain.BlockchainService;
import com.codingworld.service1.constants.Constants;
import com.codingworld.service1.model.Response;
import com.codingworld.service1.model.ChatMessageView;
import com.codingworld.service1.service.UserService;
import com.codingworld.service1.service.ChatMessageService;
import com.codingworld.service1.model.User;
import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.websocket.ChatWebSocketHandler;
import com.codingworld.service1.model.Notification;
import com.codingworld.service1.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/")
public class Service1Controller {

    @Autowired
    private UserService userService;  // loose coupling
        // UserService user= new UserService(); tight coupling

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserDao userDao;

    @PostMapping("login")
    public Response login(@RequestBody Login login){
        try {
            String email = login.getEmail();
            String password = login.getPassword();
            String publicKey = login.getPublicKey();

            // Validate input
            if (email == null || email.trim().isEmpty()) {
                return new Response("0", "Email is required", null);
            }
            if (password == null || password.trim().isEmpty()) {
                return new Response("0", "Password is required", null);
            }

            // Authenticate user using database and update public key if provided
            User authenticatedUser = userService.authenticateUser(email, password, publicKey);

            if (authenticatedUser != null) {
                // Build complete profile picture path
                String profilePicPath = buildProfilePicPath(authenticatedUser.getProfilePic());

                // Login successful - create success response with user data including UserID
                LoginResponse loginResponse = new LoginResponse(
                    authenticatedUser.getUserId(), // Added UserID to response
                    authenticatedUser.getUsername(),
                    authenticatedUser.getEmail(),
                    profilePicPath, // Complete profile pic path
                    "Welcome back!", // Welcome message
                    "active", // User status
                    true // Login success flag
                    // ethAddress removed - backend manages blockchain internally
                );

                return new Response("1", "Login successful", loginResponse);
            } else {
                // Login failed - invalid credentials
                return new Response("0", "Invalid email or password", null);
            }

        } catch (IllegalArgumentException e) {
            // Handle validation errors
            return new Response("0", e.getMessage(), null);

        } catch (Exception e) {
            // Handle database or other errors
            System.err.println("Login error: " + e.getMessage());
            return new Response("0", "Login failed due to server error", null);
        }
    }

    /**
     * Build complete profile picture path by concatenating constants path with filename from DB
     */
    private String buildProfilePicPath(String profilePicFilename) {
        if (profilePicFilename == null || profilePicFilename.trim().isEmpty()) {
            // Use default profile pic if none specified
            return Constants.PROFILE_IMAGES_PATH + File.separator + Constants.DEFAULT_PROFILE_PIC;
        }

        // Concatenate the constants path with the filename from database
        return Constants.PROFILE_IMAGES_PATH + File.separator + profilePicFilename.trim();
    }


    @GetMapping("/users")
    public Response users() {
        try {
            Response response = new Response();
            // Fetch users from database using the service layer
            List<User> usersFromDb = userService.getAllUsers();
            return new Response("1","ok", usersFromDb);

        } catch (Exception e) {
            System.err.println("Error fetching users: " + e.getMessage());
            // Return empty list in case of error
            return new Response();
        }
    }



    @PostMapping("/sendMessage")
    public Response sendMessage(@RequestBody ChatMessage message) {
        System.out.println("Received Message: " + message);

        // Resolve receiver's ETH address from DB so the blockchain event records the real recipient
        String receiverEthAddress = null;
        try {
            if (message.getTo() != null && !message.getTo().trim().isEmpty()) {
                User receiver = userDao.getUserById(message.getTo());
                if (receiver != null) {
                    receiverEthAddress = receiver.getEthAddress();
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not resolve receiver ETH address (will use deployer fallback): " + e.getMessage());
        }

        // Try to store on blockchain — non-fatal, but retry once after a contract failure
        String txHash = null;
        String address = null;
        try {
            txHash = blockchainService.storeMessageHash(message.getMessage(), receiverEthAddress);
            System.out.println("📦 Blockchain tx: " + txHash);
/*        } catch (IllegalStateException e) {
            System.err.println("⚠️ Blockchain not available (message will still be saved to DB): " + e.getMessage());*/
        } catch (Exception e) {
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            if (errMsg.contains("Hash already exists") || errMsg.contains("revert")) {
                System.err.println("⚠️ Blockchain revert (duplicate/expected): " + errMsg);
            } else if (errMsg.contains("invalid JUMP") || errMsg.contains("invalid opcode")|| errMsg.contains("Blockchain contract is not initialized")
                    || errMsg.contains("VM Exception")) {
                txHash= blockchainService.handleContractFailure();
                // Retry once on the freshly deployed contract
                try {
                    address = blockchainService.storeMessageHash(message.getMessage(), receiverEthAddress);
                    System.out.println("📦 Blockchain tx (after recovery): " + txHash);
                } catch (Exception retryEx) {

                }
            } else {
                System.err.println("⚠️ Blockchain transaction failed (message will still be saved to DB): " + errMsg);
            }
        }

        // Always save message to database regardless of blockchain result
        try {
            if(txHash==null){
                System.out.println("Blockchain server is down or transaction failed, can't send the message now.");
                return new Response("0", "Blockchain server is down at the moment please retry after some time. ", null);
            }else {
                String chatId = chatMessageService.saveChatMessage(
                        message.getMessageToSelf(),
                        message.getMessage(),
                        message.getFrom(),
                        message.getTo(),
                        message.getAlgo(),
                        txHash
                );

                System.out.println("Chat message saved with ID: " + chatId + (txHash != null ? " and txHash: " + txHash : " (no blockchain tx)"));
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to save chat message to DB: " + e.getMessage());
            return new Response("0", "Failed to save message: " + e.getMessage(), null);
        }

        return new Response("1", "ok", "Message sent successfully!");
    }

    /**
     * Verify a message against the blockchain — checks if the hash was stored and returns timestamp.
     * The request body must contain the same 'message' string that was originally sent.
     */
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


    /**
     * Get messages for a specific user using your SQL query
     * SELECT chat_id,message,`from`,`to`,algo,created_ts,tx_hash from db_chat.chat_table where `to`=1 or `from`=1
     */
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

    @PostMapping("/signUp")
    public Response signUp(@RequestBody User user) {
        try {
            boolean registered = userService.registerUser(user);
            if (registered) {
                return new Response("1", "Sign up successful", null);
            } else {
                return new Response("0", "Email already exists", null);
            }
        } catch (IllegalArgumentException e) {
            return new Response("0", e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("Sign up error: " + e.getMessage());
            return new Response("0", "Sign up failed due to server error", null);
        }
    }

    @PostMapping("/notifyUnreadableMessage")
    public Response notify(@RequestBody Notification notification) {
        try {
            notificationService.addNotification(notification);
            return new Response("1", "Notification stored successfully", null);
        } catch (Exception e) {
            System.err.println("Notification error: " + e.getMessage());
            return new Response("0", "Failed to store notification", null);
        }
    }

    @GetMapping("/api/notifications")
    public Response getAllNotifications() {
        try {
            List<Notification> notifications = notificationService.getAllNotifications();
            return new Response("1", "Notifications retrieved successfully", notifications);
        } catch (Exception e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
            return new Response("0", "Failed to fetch notifications", null);
        }
    }

    @GetMapping("/getNotifications/{userId}")
    public Response getLoginNotificationsForUser(@PathVariable String userId) {
        try {
            List<Notification> notifications = notificationService.getNotificationsByTypeAndRefId("login", userId);
            return new Response("1", "Notifications retrieved successfully", notifications);
        } catch (Exception e) {
            System.err.println("Error fetching notifications for user " + userId + ": " + e.getMessage());
            return new Response("0", "Failed to fetch notifications", null);
        }
    }

    /**
     * Verify a message against the blockchain by its chat_id.
     * Fetches the stored ciphertext from DB, recomputes keccak256, looks it up on-chain.
     * Returns the blockchain timestamp if found, or a clear "not anchored" response if tx_hash is NULL.
     */
    @GetMapping("/verifyMessage/{chatId}")
    public Response verifyMessageByChatId(@PathVariable String chatId) {
        // 1. Look up the message row in DB
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

        // 2. If tx_hash is NULL the message was never anchored (blockchain was down at send time)
        if (msg.getTxHash() == null || msg.getTxHash().trim().isEmpty()) {
            return new Response("0", "Message was not anchored on blockchain (no tx_hash)", null);
        }

        // 3. Recompute the hash from the stored ciphertext and check on-chain
        try {
            long timestamp = blockchainService.verifyMessageHash(msg.getMessage());
            if (timestamp > 0) {
                java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
                result.put("chatId",    chatId);
                result.put("txHash",    msg.getTxHash());
                result.put("from",      msg.getFromUser());
                result.put("to",        msg.getToUser());
                result.put("timestamp", timestamp);
                result.put("verified",  true);
                return new Response("1", "Message is verified on blockchain", result);
            } else {
                return new Response("0", "Message hash not found on blockchain (possible tampering)", null);
            }
        } catch (IllegalStateException e) {
            return new Response("0", "Blockchain not available: " + e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("Blockchain verification error for chatId " + chatId + ": " + e.getMessage());
            return new Response("0", "Verification failed: " + e.getMessage(), null);
        }
    }

}
