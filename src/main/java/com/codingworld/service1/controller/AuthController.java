package com.codingworld.service1.controller;

import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.model.Response;
import com.codingworld.service1.model.User;
import com.codingworld.service1.model.dto.LoginRequest;
import com.codingworld.service1.model.dto.LoginResponse;
import com.codingworld.service1.service.UserPresenceService;
import com.codingworld.service1.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserPresenceService userPresenceService;

    @Autowired
    private UserDao userDao;

    @PostMapping("login")
    public Response login(@RequestBody LoginRequest login) {
        try {
            String email = login.getEmail();
            String password = login.getPassword();
            String publicKey = login.getPublicKey();

            if (email == null || email.trim().isEmpty()) {
                return new Response("0", "Email is required", null);
            }
            if (password == null || password.trim().isEmpty()) {
                return new Response("0", "Password is required", null);
            }

            User authenticatedUser = userService.authenticateUser(email, password, publicKey);

            if (authenticatedUser != null) {
                // Optional: persist FCM token if provided
                if (login.getFcmToken() != null && !login.getFcmToken().isBlank()) {
                    try {
                        userDao.updateFcmToken(authenticatedUser.getUserId(), login.getFcmToken(), login.getPlatform());
                    } catch (Exception ignored) { }
                }

                LoginResponse loginResponse = new LoginResponse(
                        authenticatedUser.getUserId(),
                        authenticatedUser.getUsername(),
                        authenticatedUser.getEmail(),
                        authenticatedUser.getProfilePic(),
                        "Welcome back!",
                        "active",
                        true
                );
                return new Response("1", "Login successful", loginResponse);
            } else {
                return new Response("0", "Invalid email or password", null);
            }

        } catch (IllegalArgumentException e) {
            return new Response("0", e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            return new Response("0", "Login failed due to server error", null);
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

    @GetMapping("/users")
    public Response users() {
        try {
            List<User> users = userService.getAllUsers();

            List<Map<String, Object>> usersWithPresence = users.stream().map(user -> {
                Map<String, Object> userMap = new LinkedHashMap<>();
                userMap.put("userId",    user.getUserId());
                userMap.put("username",  user.getUsername());
                userMap.put("email",     user.getEmail());
                userMap.put("mobileno",  user.getMobileno());
                userMap.put("publicKey", user.getPublicKey());
                userMap.put("profilePic", user.getProfilePic());
                userMap.put("isOnline",  userPresenceService.isOnline(user.getUserId()));
                userMap.put("lastSeen",  userPresenceService.getLastSeen(user.getUserId()));
                return userMap;
            }).toList();

            Response response = new Response("1", "ok", usersWithPresence);
            System.out.println("[INFO] /users API response: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("Error fetching users: " + e.getMessage());
            Response errorResponse = new Response("0", "Failed to fetch users", null);
            System.out.println("[INFO] /users API response: " + errorResponse);
            return errorResponse;
        }
    }

    /**
     * GET /user/presence/{userId}
     * Returns the real-time online status and last seen of a specific user.
     */
    @GetMapping("/user/presence/{userId}")
    public Response getUserPresence(@PathVariable String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return new Response("0", "userId is required", null);
            }
            Map<String, Object> presence = userPresenceService.getPresence(userId);
            System.out.println("[INFO] /user/presence/" + userId + " response: " + presence);
            return new Response("1", "ok", presence);
        } catch (Exception e) {
            System.err.println("Error fetching presence for user " + userId + ": " + e.getMessage());
            return new Response("0", "Failed to fetch presence", null);
        }
    }

    @PostMapping("/updateProfilePic")
    public Response updateProfilePic(
            @RequestParam("userId") String userId,
            @RequestParam("file") MultipartFile file) {
        try {
            String base64Image = userService.updateProfilePic(userId, file);
            return new Response("1", "Profile picture updated successfully", base64Image);
        } catch (IllegalArgumentException e) {
            return new Response("0", e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("Update profile pic error: " + e.getMessage());
            return new Response("0", "Failed to update profile picture", null);
        }
    }
}
