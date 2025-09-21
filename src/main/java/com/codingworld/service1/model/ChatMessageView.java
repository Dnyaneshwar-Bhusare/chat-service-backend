package com.codingworld.service1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model for retrieving chat messages from database
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageView {
    private String chatId;
    private String message;
    private String fromUser;
    private String toUser;
    private String algo;
    private LocalDateTime createdTs;
    private String txHash;
}
