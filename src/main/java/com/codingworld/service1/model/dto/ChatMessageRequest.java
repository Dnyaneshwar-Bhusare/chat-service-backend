package com.codingworld.service1.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessageRequest {
    private String message;
    private String messageToSelf;
    private String from;
    private String to;
    private String algo;
    @JsonIgnore
    private String secretKey;
    private String timestamp;
}

