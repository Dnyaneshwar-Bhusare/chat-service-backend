package com.codingworld.service1.controller;

import com.codingworld.service1.dao.UserDao;
import com.codingworld.service1.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/")
public class FcmController {

    @Autowired
    private UserDao userDao;

    @PostMapping("/updateFcmToken")
    public Response updateFcmToken(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String fcmToken = request.get("fcmToken");
        String platform = request.get("platform");

        if (userId == null || userId.trim().isEmpty()) {
            return new Response("0", "userId is required", null);
        }

        int rows;
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            rows = userDao.updateFcmToken(userId, null, null);
        } else {
            rows = userDao.updateFcmToken(userId, fcmToken, platform);
        }

        if (rows > 0) {
            return new Response("1", "FCM token updated", null);
        } else {
            return new Response("0", "User not found", null);
        }
    }
}

