package com.codingworld.service1.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String userId;
    private String userName;
    private String email;
    private String profilePic;
    private String status;
    private String userFlag;
    private Boolean isUserActive;
}

