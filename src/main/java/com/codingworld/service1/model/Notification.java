package com.codingworld.service1.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String notificationType;
    private String notificationRefId;
    private String timestamp;
    private String message;
}
