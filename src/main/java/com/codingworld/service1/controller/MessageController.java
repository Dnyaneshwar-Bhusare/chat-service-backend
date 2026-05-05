package com.codingworld.service1.controller;

import com.codingworld.service1.dao.ChatMessageDao;
import com.codingworld.service1.model.EditMessageRequest;
import com.codingworld.service1.model.ChatMessageView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MessageController {

    @Autowired
    private ChatMessageDao chatMessageDao;

    @PutMapping("/editMessage/{chatId}")
    public ResponseEntity<?> editMessage(@PathVariable String chatId, @RequestBody EditMessageRequest req) {
        try {
            // Validate required fields
            if (req.getUserId() == null || req.getUserId().trim().isEmpty() || req.getMessage() == null || req.getMessage().trim().isEmpty() || req.getTimestamp() == null || req.getTimestamp().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("Missing required fields"));
            }

            ChatMessageView message = chatMessageDao.getMessageByChatId(chatId);
            if (message == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Message not found"));
            }

            if (!req.getUserId().equals(message.getFromUser())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Unauthorized: you can only edit your own messages"));
            }

            LocalDateTime updatedTs;
            try {
                updatedTs = LocalDateTime.parse(req.getTimestamp().replace("Z", ""));
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("Invalid timestamp format"));
            }

            boolean updated = chatMessageDao.updateMessageByChatId(
                    chatId,
                    req.getMessage(),
                    req.getMessageToSelf(),
                    updatedTs
            );
            if (!updated) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("Internal server error"));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("chatId", chatId);
            data.put("edited", true);
            data.put("updatedTs", req.getTimestamp());
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "1");
            resp.put("message", "Message updated successfully");
            resp.put("data", data);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("Internal server error"));
        }
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "0");
        resp.put("message", msg);
        return resp;
    }
}

