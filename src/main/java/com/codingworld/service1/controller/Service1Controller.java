package com.codingworld.service1.controller;

import com.codingworld.service1.blockchain.BlockchainService;
import com.codingworld.service1.constants.Constants;
import com.codingworld.service1.model.Response;
import com.codingworld.service1.model.ChatMessageView;
import com.codingworld.service1.service.UserService;
import com.codingworld.service1.service.ChatMessageService;
import com.codingworld.service1.model.User;
import com.codingworld.service1.utils.CryptoHelper;
import com.codingworld.service1.websocket.ChatWebSocketHandler;
import com.codingworld.service1.model.Notification;
import com.codingworld.service1.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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


    @GetMapping("/friends")
    public ChatList getFriends() {
        
        List<String> list = Arrays.asList("John Doe", "Alice Smith", "Bob Johnson", "Emma Brown", "Charlie Davis","Dnyaneshwar Bhusare","John Doe", "Alice Smith", "Bob Johnson", "Emma Brown", "Charlie Davis","Dnyaneshwar Bhusare","John Doe", "Alice Smith", "Bob Johnson", "Emma Brown", "Charlie Davis","Dnyaneshwar Bhusare","John Doe", "Alice Smith", "Bob Johnson", "Emma Brown", "Charlie Davis","Dnyaneshwar Bhusare");
        ChatList chatList = new ChatList();
        chatList.setFriends(list);
        return chatList;
    }

    @PostMapping("/sendMessage")
    public Response sendMessage(@RequestBody ChatMessage message) {
        String decrypt = CryptoHelper.decrypt(message.getMessage(), message.getAlgo());
        System.out.println("Received Message: " + message);
        System.out.println("Decrypted msg: " + decrypt);

        // Try to store on blockchain — but treat it as non-fatal
        String txHash = null;
        try {
            txHash = blockchainService.storeMessageHash(message.getMessage());
            System.out.println("📦 Blockchain tx: " + txHash);
        } catch (IllegalStateException e) {
            // Contract not initialized (Ganache down or init failed) — just skip blockchain
            System.err.println("⚠️ Blockchain not available (message will still be saved to DB): " + e.getMessage());
        } catch (Exception e) {
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            // Contract reverts (e.g. "Hash already exists") are expected — do NOT kill the contract
            if (errMsg.contains("Hash already exists") || errMsg.contains("revert")) {
                System.err.println("⚠️ Blockchain revert (duplicate/expected): " + errMsg);
            } else if (errMsg.contains("invalid JUMP") || errMsg.contains("invalid opcode")
                    || errMsg.contains("VM Exception")) {
                // Genuine EVM/infrastructure failure — clear stale contract so it redeploys
                //System.err.println("⚠️ Blockchain infrastructure failure — clearing contract: " + errMsg);
                blockchainService.handleContractFailure();
            } else {
                // Unknown error — log it but do NOT kill the contract
                System.err.println("⚠️ Blockchain transaction failed (message will still be saved to DB): " + errMsg);
            }
        }

        // Always save message to database regardless of blockchain result
        try {
            String chatId = chatMessageService.saveChatMessage(
                    message.getMessage(),
                    message.getFrom(),
                    message.getTo(),
                    message.getAlgo(),
                    txHash   // null if blockchain failed — that's OK
            );
            System.out.println("Chat message saved with ID: " + chatId + (txHash != null ? " and txHash: " + txHash : " (no blockchain tx)"));
        } catch (Exception e) {
            System.err.println("❌ Failed to save chat message to DB: " + e.getMessage());
            return new Response("0", "Failed to save message: " + e.getMessage(), null);
        }

        return new Response("1", "ok", "Message sent successfully!");
    }

    /**
     * Verify a message against the blockchain — checks if the hash was stored and returns timestamp
     */
    @PostMapping("/verifyMessage")
    public Response verifyMessage(@RequestBody ChatMessage message) {
        try {
            long timestamp = blockchainService.verifyMessageHash(message.getMessage());
            if (timestamp > 0) {
                return new Response("1", "Message is verified on blockchain", timestamp);
            } else {
                return new Response("0", "Message hash not found on blockchain", null);
            }
        } catch (Exception e) {
            System.err.println("Blockchain verification error: " + e.getMessage());
            return new Response("0", "Verification failed: " + e.getMessage(), null);
        }
    }

    @GetMapping("/getMessages")
    public List<GetMessages> getMesages(@RequestParam("from") String from) {
        System.out.println("getting all the messages from "+from);
        List<GetMessages> messages= new ArrayList<>();
        GetMessages messages1=new GetMessages();
        GetMessages messages2 = new GetMessages();
        messages1.setSent(true);
        messages1.setMessage("hi");
        messages1.setTs("yesterday");
        messages2.setSent(false);
        messages2.setMessage("hello");
        messages2.setTs("yesterday");
        messages.add(messages1);
        messages.add(messages2);
        return messages;
    }

    /**
     * Get messages for a specific user using your SQL query
     * SELECT chat_id,message,`from`,`to`,algo,created_ts,tx_hash from db_chat.chat_table where `to`=1 or `from`=1
     */
    @GetMapping("/getUserMessages/{userId}")
    public Response getUserMessages(@PathVariable String userId) {
        try {
            List<ChatMessageView> messages = chatMessageService.getMessagesForUser(userId);
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

}
