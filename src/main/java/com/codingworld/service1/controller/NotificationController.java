package com.codingworld.service1.controller;

import com.codingworld.service1.model.Notification;
import com.codingworld.service1.model.Response;
import com.codingworld.service1.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/notifyUnreadableMessage")
    public Response notify(@RequestBody Notification notification) {
        try {
            notificationService.addNotification(notification);
            return new Response("1", "Notification stored successfully", null);
        } catch (Exception e) {
            System.err.println("Notification error: " + e.getMessage());
            return new Response("0", "Failed to store notification", null);
        }
    }

    @GetMapping("/api/notifications")
    public Response getAllNotifications() {
        try {
            List<Notification> notifications = notificationService.getAllNotifications();
            return new Response("1", "Notifications retrieved successfully", notifications);
        } catch (Exception e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
            return new Response("0", "Failed to fetch notifications", null);
        }
    }

    @GetMapping("/getNotifications/{userId}")
    public Response getNotificationsForUser(@PathVariable String userId) {
        try {
            List<Notification> notifications = notificationService.getNotificationsByTypeAndRefId("login", userId);
            return new Response("1", "Notifications retrieved successfully", notifications);
        } catch (Exception e) {
            System.err.println("Error fetching notifications for user " + userId + ": " + e.getMessage());
            return new Response("0", "Failed to fetch notifications", null);
        }
    }
}

