package com.codingworld.service1.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatListRequest {
    private List<String> friends;
}
