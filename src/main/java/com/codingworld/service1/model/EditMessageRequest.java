package com.codingworld.service1.model;

import lombok.Data;

@Data
public class EditMessageRequest {
    private String userId;
    private String message;
    private String messageToSelf;
    private String timestamp;
}

