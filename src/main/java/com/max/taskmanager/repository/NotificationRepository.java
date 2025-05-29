package com.max.taskmanager.repository;

import com.max.taskmanager.model.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id); // Может понадобиться для маркировки как прочитанное
    List<Notification> findAllByUserId(Long userId);
    List<Notification> findAllByUserIdAndReadFalse(Long userId);
}