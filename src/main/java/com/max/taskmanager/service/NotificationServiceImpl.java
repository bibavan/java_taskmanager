package com.max.taskmanager.service;

import com.max.taskmanager.model.Notification;
import com.max.taskmanager.repository.NotificationRepository;
import com.max.taskmanager.repository.UserRepository; // Для проверки существования пользователя
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Notification createNotification(Long userId, String message) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found. Cannot create notification."));

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setCreationDate(LocalDateTime.now());
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getAllUserNotifications(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found."));
        return notificationRepository.findAllByUserId(userId);
    }

    @Override
    public List<Notification> getPendingUserNotifications(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found."));
        return notificationRepository.findAllByUserIdAndReadFalse(userId);
    }

    @Override
    public Optional<Notification> markAsRead(Long notificationId, Long userId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            if (notification.getUserId().equals(userId) && !notification.isRead()) {
                notification.setRead(true);
                return Optional.of(notificationRepository.save(notification));
            }
        }
        return Optional.empty();
    }
}