package com.codingworld.service1.controller;

import com.codingworld.service1.model.Response;
import com.codingworld.service1.model.User;
import com.codingworld.service1.model.dto.LoginRequest;
import com.codingworld.service1.model.dto.LoginResponse;
import com.codingworld.service1.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/")
public class AuthController {

    @Autowired
    private UserService userService;

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
            Response response = new Response("1", "ok", userService.getAllUsers());
            System.out.println("[INFO] /users API response: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("Error fetching users: " + e.getMessage());
            Response errorResponse = new Response("0", "Failed to fetch users", null);
            System.out.println("[INFO] /users API response: " + errorResponse);
            return errorResponse;
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
