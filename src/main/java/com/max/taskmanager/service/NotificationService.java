package com.max.taskmanager.service;

import com.max.taskmanager.model.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationService {
    Notification createNotification(Long userId, String message);
    List<Notification> getAllUserNotifications(Long userId);
    List<Notification> getPendingUserNotifications(Long userId); // "Pending" here means "unread"
    Optional<Notification> markAsRead(Long notificationId, Long userId);
}