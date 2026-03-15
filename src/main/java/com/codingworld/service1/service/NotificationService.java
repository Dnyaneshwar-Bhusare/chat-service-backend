package com.codingworld.service1.service;

import com.codingworld.service1.dao.NotificationDao;
import com.codingworld.service1.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    @Autowired
    private NotificationDao notificationDao;

    public void addNotification(Notification notification) {
        notificationDao.insertNotification(notification);
    }

    public List<Notification> getAllNotifications() {
        return notificationDao.getAllNotifications();
    }

    public List<Notification> getNotificationsByTypeAndRefId(String type, String refId) {
        return notificationDao.getNotificationsByTypeAndRefId(type, refId);
    }
}
