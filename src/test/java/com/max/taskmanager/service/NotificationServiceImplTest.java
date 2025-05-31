package com.max.taskmanager.service;

import com.max.taskmanager.model.Notification;
import com.max.taskmanager.model.User;
import com.max.taskmanager.repository.NotificationRepository;
import com.max.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Notification testNotification1;
    private Notification testNotification2;


    @BeforeEach
    void setUp() {
        testUser = new User(1L, "testuser", "password");

        testNotification1 = new Notification(1L, 1L, "Notification 1");
        testNotification1.setRead(false);
        testNotification1.setCreationDate(LocalDateTime.now().minusHours(1));

        testNotification2 = new Notification(2L, 1L, "Notification 2");
        testNotification2.setRead(true);
        testNotification2.setCreationDate(LocalDateTime.now().minusHours(2));
    }

    @Test
    void createNotification_whenUserExists_shouldSaveAndReturnNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            if (n.getId() == null) n.setId(3L);
            // n.setCreationDate(LocalDateTime.now()); // This is set in service impl
            return n;
        });

        Notification result = notificationService.createNotification(1L, "New Notification");

        assertNotNull(result);
        assertEquals("New Notification", result.getMessage());
        assertFalse(result.isRead());
        assertNotNull(result.getCreationDate()); // Verifies creation date is set by service
        verify(userRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_whenUserDoesNotExist_shouldThrowIllegalArgumentException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            notificationService.createNotification(99L, "Notification for non-existent user");
        });
        assertEquals("User with id 99 not found. Cannot create notification.", exception.getMessage());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void getAllUserNotifications_whenUserExists_shouldReturnAllNotifications() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.findAllByUserId(1L)).thenReturn(Arrays.asList(testNotification1, testNotification2));

        List<Notification> result = notificationService.getAllUserNotifications(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testNotification1));
        assertTrue(result.contains(testNotification2));
        verify(userRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).findAllByUserId(1L);
    }

    @Test
    void getPendingUserNotifications_whenUserExists_shouldReturnUnreadNotifications() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.findAllByUserIdAndReadFalse(1L))
                .thenReturn(Collections.singletonList(testNotification1));

        List<Notification> result = notificationService.getPendingUserNotifications(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testNotification1, result.get(0));
        verify(userRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).findAllByUserIdAndReadFalse(1L);
    }

    @Test
    void markAsRead_whenNotificationExistsAndBelongsToUserAndUnread_shouldMarkAsRead() {
        // Ensure testNotification1 is unread for this test
        testNotification1.setRead(false);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification1)); 
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification1);

        Optional<Notification> result = notificationService.markAsRead(1L, 1L);

        assertTrue(result.isPresent());
        assertTrue(result.get().isRead());
        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(testNotification1);
    }

    @Test
    void markAsRead_whenNotificationAlreadyRead_shouldReturnEmptyOptionalWithoutSaving() {
        // testNotification2 is already read
        when(notificationRepository.findById(2L)).thenReturn(Optional.of(testNotification2));

        Optional<Notification> result = notificationService.markAsRead(2L, 1L);

        assertFalse(result.isPresent());
        verify(notificationRepository, times(1)).findById(2L);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAsRead_whenNotificationNotFound_shouldReturnEmpty() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Notification> result = notificationService.markAsRead(99L, 1L);

        assertFalse(result.isPresent());
        verify(notificationRepository, times(1)).findById(99L); // findById is called
        verify(notificationRepository, never()).save(any(Notification.class)); // but save is not
    }

    @Test
    void markAsRead_whenNotificationDoesNotBelongToUser_shouldReturnEmpty() {
        Notification otherUserNotification = new Notification(3L, 2L, "Other user's notification");
        otherUserNotification.setRead(false);
        when(notificationRepository.findById(3L)).thenReturn(Optional.of(otherUserNotification));

        Optional<Notification> result = notificationService.markAsRead(3L, 1L); // User 1 tries to read User 2's notification

        assertFalse(result.isPresent());
        verify(notificationRepository, times(1)).findById(3L);
        verify(notificationRepository, never()).save(any(Notification.class));
    }
} 