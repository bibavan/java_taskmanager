package com.max.taskmanager.service;

import com.max.taskmanager.model.Notification;
import com.max.taskmanager.repository.NotificationRepository;
import com.max.taskmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
// No @Profile annotation here, this service is always active
// The behavior of createNotification will depend on the active profile
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Environment environment;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository, 
                                 UserRepository userRepository, 
                                 Environment environment) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.environment = environment;
    }

    @Override
    public Notification createNotification(Long userId, String message) {
        // Check if 'rabbitmq' profile is active
        boolean rabbitMqProfileActive = Arrays.asList(environment.getActiveProfiles()).contains("rabbitmq");

        if (rabbitMqProfileActive) {
            logger.warn("Notification creation via NotificationService.createNotification() is disabled when 'rabbitmq' profile is active. Notifications should be generated via RabbitMQ messages.");
            // Optionally, throw an exception or return null/empty
            // For now, just returning null as an example of preventing creation
            return null; 
        }

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