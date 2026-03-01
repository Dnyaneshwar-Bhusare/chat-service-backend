package com.codingworld.service1.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String userId; // Changed from Long to String
    private String userName;
    private String email;
    private String profilePic;
    private String status;
    private String userFlag;
    private Boolean isUserActive;
    private String ethAddress; // Ethereum address from Ganache for blockchain operations
}
