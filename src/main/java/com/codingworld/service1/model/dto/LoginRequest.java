package com.codingworld.service1.model.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private String publicKey;
    // Optional — Flutter client may send these after login
    private String fcmToken;
    private String platform;
}
