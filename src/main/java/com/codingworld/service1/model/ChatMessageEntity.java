package com.codingworld.service1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Chat message model for database storage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEntity {
    private String chatId;
    private String message;
    private String fromUser;
    private String toUser;
    private String algo;
    private String txHash;
    private LocalDateTime createdTs;

    // Constructor without chatId and timestamp (auto-generated)
    public ChatMessageEntity(String message, String fromUser, String toUser, String algo, String txHash) {
        this.message = message;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.algo = algo;
        this.txHash = txHash;
        this.createdTs = LocalDateTime.now();
    }
}
