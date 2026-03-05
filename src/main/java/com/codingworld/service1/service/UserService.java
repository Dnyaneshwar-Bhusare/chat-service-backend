package com.codingworld.service1.service;

import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for User operations
 */
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Get all users from the database
     * @return List of User objects
     */
    public List<User> getAllUsers() {
        try {
            return userDao.getAllUsers();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users from database: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by email
     * @param email User's email
     * @return User object or null if not found
     */
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

    /**
     * Get user by username
     * @param username User's username
     * @return User object or null if not found
     */
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

    /**
     * Check if user exists by email
     * @param email User's email
     * @return true if user exists, false otherwise
     */
    public boolean userExistsByEmail(String email) {
        return getUserByEmail(email) != null;
    }

    /**
     * Get total user count
     * @return Number of users in the database
     */
    public int getUserCount() {
        return getAllUsers().size();
    }

    /**
     * Authenticate user login credentials
     * @param email User's email
     * @param password User's password
     * @return User object if login successful, null if failed
     */
    public User authenticateUser(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            User userCredentials = userDao.getUserCredentials(email.trim().toLowerCase());

            if (userCredentials == null) {
                return null; // User not found
            }

            String storedPassword = userCredentials.getPassword();
            boolean passwordMatches = false;

            if (isBCryptHash(storedPassword)) {
                // ✅ Already BCrypt — use BCrypt compare
                passwordMatches = passwordEncoder.matches(password, storedPassword);
            } else {
                // ⚠️ Plain text password (legacy user) — compare directly
                passwordMatches = password.equals(storedPassword);
                if (passwordMatches) {
                    // ✅ Auto-migrate: hash and update in DB immediately
                    String hashed = passwordEncoder.encode(password);
                    userDao.updatePassword(email.trim().toLowerCase(), hashed);
                    System.out.println("🔄 Password migrated to BCrypt for: " + email);
                }
            }

            if (passwordMatches) {
                User fullUserDetails = userDao.getUserForLogin(email.trim().toLowerCase());
                if (fullUserDetails != null && fullUserDetails.getUserId() == null) {
                    fullUserDetails.setUserId(userCredentials.getUserId());
                }
                return fullUserDetails;
            }

            return null; // Password doesn't match

        } catch (Exception e) {
            throw new RuntimeException("Failed to authenticate user: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a stored password is already a BCrypt hash.
     * BCrypt hashes always start with $2a$, $2b$, or $2y$
     */
    private boolean isBCryptHash(String password) {
        return password != null &&
               (password.startsWith("$2a$") ||
                password.startsWith("$2b$") ||
                password.startsWith("$2y$"));
    }

    /**
     * Get user details for successful login
     * @param email User's email
     * @return User object with full details
     */
    public User getUserForLogin(String email) {
        try {
            return userDao.getUserForLogin(email);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get user for login: " + e.getMessage(), e);
        }
    }

    /**
     * Register a new user (sign up)
     * @param user User object with details
     * @return true if registration successful, false if email already exists
     */
    public boolean registerUser(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (userExistsByEmail(user.getEmail())) {
            return false; // Email already exists
        }
        // ✅ Hash password with BCrypt before saving to DB
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userDao.insertUser(user);
    }
}
