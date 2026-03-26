package com.codingworld.service1.service;

import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        try {
            return userDao.getAllUsers();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users from database: " + e.getMessage(), e);
        }
    }

    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        try {
            return userDao.getUserByEmail(email.trim().toLowerCase());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user by email: " + e.getMessage(), e);
        }
    }

    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        try {
            return userDao.getUserByUsername(username.trim());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user by username: " + e.getMessage(), e);
        }
    }

    public boolean userExistsByEmail(String email) {
        return getUserByEmail(email) != null;
    }

    public int getUserCount() {
        return getAllUsers().size();
    }

    public User authenticateUser(String email, String password, String publicKey) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            User userCredentials = userDao.getUserCredentials(email.trim().toLowerCase());
            if (userCredentials == null) {
                return null;
            }

            String storedPassword = userCredentials.getPassword();
            boolean passwordMatches;

            if (isBCryptHash(storedPassword)) {
                passwordMatches = passwordEncoder.matches(password, storedPassword);
            } else {
                passwordMatches = password.equals(storedPassword);
                if (passwordMatches) {
                    String hashed = passwordEncoder.encode(password);
                    userDao.updatePassword(email.trim().toLowerCase(), hashed);
                }
            }

            if (passwordMatches) {
                if (publicKey != null && !publicKey.trim().isEmpty()) {
                    userDao.updateUserPublicKey(email.trim().toLowerCase(), publicKey);
                }
                return userDao.getUserForLogin(email.trim().toLowerCase());
            }
            return null;

        } catch (Exception e) {
            System.err.println("Authentication error for " + email + ": " + e.getMessage());
            return null;
        }
    }

    private boolean isBCryptHash(String password) {
        return password != null &&
               (password.startsWith("$2a$") ||
                password.startsWith("$2b$") ||
                password.startsWith("$2y$"));
    }

    public boolean registerUser(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (userExistsByEmail(user.getEmail())) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userDao.insertUser(user);
    }

    public String updateProfilePic(String userId, MultipartFile file) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be null or empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        try {
            byte[] bytes = file.getBytes();
            String base64 = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            boolean updated = userDao.updateProfilePic(userId.trim(), base64);
            if (!updated) {
                throw new RuntimeException("User not found or profile pic update failed");
            }
            return base64;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image file: " + e.getMessage(), e);
        }
    }
}
