package com.max.taskmanager.repository;

import com.max.taskmanager.model.Notification;
import com.max.taskmanager.repository.impl.InMemoryNotificationRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryNotificationRepositoryTest {

    private NotificationRepository notificationRepository;

    private final Long userId1 = 1L;
    private final Long userId2 = 2L;

    @BeforeEach
    void setUp() {
        notificationRepository = new InMemoryNotificationRepositoryImpl();
    }

    @Test
    void save_newNotification_shouldAssignIdAndStore() {
        Notification notification = new Notification(userId1, "Test Message 1");
        Notification savedNotification = notificationRepository.save(notification);

        assertNotNull(savedNotification.getId());
        assertEquals(userId1, savedNotification.getUserId());
        assertEquals("Test Message 1", savedNotification.getMessage());
        assertFalse(savedNotification.isRead());

        Optional<Notification> found = notificationRepository.findById(savedNotification.getId());
        assertTrue(found.isPresent());
        assertEquals(savedNotification, found.get());
    }

    @Test
    void save_existingNotification_shouldUpdate() {
        Notification notification = notificationRepository.save(new Notification(userId1, "Original Message"));
        Long originalId = notification.getId();

        notification.setMessage("Updated Message");
        notification.setRead(true);
        Notification updatedNotification = notificationRepository.save(notification);

        assertEquals(originalId, updatedNotification.getId());
        assertEquals("Updated Message", updatedNotification.getMessage());
        assertTrue(updatedNotification.isRead());

        Optional<Notification> found = notificationRepository.findById(originalId);
        assertTrue(found.isPresent());
        assertEquals("Updated Message", found.get().getMessage());
        assertTrue(found.get().isRead());
    }

    @Test
    void findById_whenNotificationExists_shouldReturnNotification() {
        Notification notification = notificationRepository.save(new Notification(userId1, "Find Me"));
        Optional<Notification> found = notificationRepository.findById(notification.getId());
        assertTrue(found.isPresent());
        assertEquals(notification, found.get());
    }

    @Test
    void findById_whenNotificationDoesNotExist_shouldReturnEmpty() {
        Optional<Notification> found = notificationRepository.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void findAllByUserId_shouldReturnAllUserNotifications() {
        Notification n1_u1 = notificationRepository.save(new Notification(userId1, "U1N1"));
        Notification n2_u1 = notificationRepository.save(new Notification(userId1, "U1N2"));
        notificationRepository.save(new Notification(userId2, "U2N1"));

        List<Notification> user1Notifications = notificationRepository.findAllByUserId(userId1);
        assertEquals(2, user1Notifications.size());
        assertTrue(user1Notifications.contains(n1_u1));
        assertTrue(user1Notifications.contains(n2_u1));

        List<Notification> user2Notifications = notificationRepository.findAllByUserId(userId2);
        assertEquals(1, user2Notifications.size());
    }

    @Test
    void findAllByUserIdAndReadFalse_shouldReturnOnlyUnreadUserNotifications() {
        Notification unread1_u1 = new Notification(userId1, "U1 Unread 1");
        unread1_u1.setRead(false);
        notificationRepository.save(unread1_u1);

        Notification read_u1 = new Notification(userId1, "U1 Read");
        read_u1.setRead(true);
        notificationRepository.save(read_u1);

        Notification unread_u2 = new Notification(userId2, "U2 Unread");
        unread_u2.setRead(false);
        notificationRepository.save(unread_u2);

        List<Notification> user1Unread = notificationRepository.findAllByUserIdAndReadFalse(userId1);
        assertEquals(1, user1Unread.size());
        assertTrue(user1Unread.contains(unread1_u1));
        assertFalse(user1Unread.stream().anyMatch(n -> n.getId().equals(read_u1.getId())));

        List<Notification> user2Unread = notificationRepository.findAllByUserIdAndReadFalse(userId2);
        assertEquals(1, user2Unread.size());
        assertTrue(user2Unread.contains(unread_u2));
    }
} 