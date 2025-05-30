package com.max.taskmanager.controller;

import com.max.taskmanager.model.Notification;
import com.max.taskmanager.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(@PathVariable Long userId, @RequestBody Notification notificationRequest) {
        try {
            Notification createdNotification = notificationService.createNotification(userId, notificationRequest.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdNotification);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getAllUserNotifications(@PathVariable Long userId) {
         try {
            List<Notification> notifications = notificationService.getAllUserNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (IllegalArgumentException e) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Notification>> getPendingUserNotifications(@PathVariable Long userId) {
        try {
            List<Notification> notifications = notificationService.getPendingUserNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (IllegalArgumentException e) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markNotificationAsRead(@PathVariable Long userId, @PathVariable Long notificationId) {
        Notification notification = notificationService.markAsRead(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found or access denied"));
        return ResponseEntity.ok(notification);
    }
} 