package com.max.taskmanager.repository;

import com.max.taskmanager.model.Notification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryNotificationRepository implements NotificationRepository {
    private final Map<Long, Notification> notifications = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Notification save(Notification notification) {
        if (notification.getId() == null) {
            notification.setId(idGenerator.incrementAndGet());
        }
        notifications.put(notification.getId(), notification);
        return notification;
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return Optional.ofNullable(notifications.get(id));
    }

    @Override
    public List<Notification> findAllByUserId(Long userId) {
        return notifications.values().stream()
                .filter(notification -> notification.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findAllByUserIdAndReadFalse(Long userId) {
        return notifications.values().stream()
                .filter(notification -> notification.getUserId().equals(userId) && !notification.isRead())
                .collect(Collectors.toList());
    }
}