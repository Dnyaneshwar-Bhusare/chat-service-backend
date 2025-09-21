package com.codingworld.service1.service;

import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for User operations
 */
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

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
            // Get user credentials from database
            User userCredentials = userDao.getUserCredentials(email.trim().toLowerCase());

            if (userCredentials == null) {
                return null; // User not found
            }

            // Validate password (in real app, you'd hash the password)
            if (password.equals(userCredentials.getPassword())) {
                // Password matches, get full user details for response
                User fullUserDetails = userDao.getUserForLogin(email.trim().toLowerCase());

                // Ensure UserID is set (get it from credentials if needed)
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
}
