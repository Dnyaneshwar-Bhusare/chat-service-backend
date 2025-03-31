package com.codingworld.service1.controller;

import org.bouncycastle.math.ec.rfc7748.X448;

import java.util.List;

public class ChatList {
    private  List<String> friends;

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }
}
