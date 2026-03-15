package com.codingworld.service1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User model class representing user data from the database
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String userId;  // Changed from Long to String
    private String username;
    private String email;
    private String mobileno;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String profilePic;
    private String ethAddress; // Ethereum address from Ganache
    private String publicKey;
    // Constructor without password and profilePic for basic user info
    public User(String username, String email, String mobileno) {
        this.username = username;
        this.email = email;
        this.mobileno = mobileno;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", mobileno='" + mobileno + '\'' +
                ", profilePic='" + profilePic + '\'' +
                ", ethAddress='" + ethAddress + '\'' +
                '}';
    }
}
