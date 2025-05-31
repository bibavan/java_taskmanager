package com.max.taskmanager.service;

import com.max.taskmanager.model.Notification;
import com.max.taskmanager.model.User;
import com.max.taskmanager.repository.NotificationRepository;
import com.max.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

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

    @Mock
    private Environment environment; 

    // No @InjectMocks, will initialize manually
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Notification testNotification1;
    private Notification testNotification2;


    @BeforeEach
    void setUp() {
        testUser = new User(1L, "testuser", "password");
        
        // Manual initialization
        notificationService = new NotificationServiceImpl(notificationRepository, userRepository, environment);

        testNotification1 = new Notification(testUser.getId(), "Notification 1");
        testNotification1.setId(1L);
        testNotification1.setRead(false);
        testNotification1.setCreationDate(LocalDateTime.now().minusHours(1));

        testNotification2 = new Notification(testUser.getId(), "Notification 2");
        testNotification2.setId(2L);
        testNotification2.setRead(true);
        testNotification2.setCreationDate(LocalDateTime.now().minusHours(2));
    }

    @Test
    void createNotification_whenUserExists_andRabbitMQProfileNotActive_shouldSaveAndReturnNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(environment.getActiveProfiles()).thenReturn(new String[]{}); // RabbitMQ profile is NOT active
        
        Notification toSave = new Notification();
        toSave.setUserId(1L);
        toSave.setMessage("New Notification");
        // id, creationDate, read are set by service or save operation

        Notification savedNotification = new Notification(1L, "New Notification");
        savedNotification.setId(3L);
        savedNotification.setCreationDate(LocalDateTime.now());
        savedNotification.setRead(false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        Notification result = notificationService.createNotification(1L, "New Notification");

        assertNotNull(result);
        assertEquals(savedNotification.getId(), result.getId());
        assertEquals("New Notification", result.getMessage());
        assertFalse(result.isRead());
        assertNotNull(result.getCreationDate());
        verify(userRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_whenUserExists_andRabbitMQProfileActive_shouldReturnNullAndNotSave() {
        // No need to mock userRepository.findById for this specific path, as it won't be called if profile check is first
        // Correction: profile check is first, so findById for user is NOT called if rabbitmq is active.
        when(environment.getActiveProfiles()).thenReturn(new String[]{"rabbitmq"}); // RabbitMQ profile IS active

        Notification result = notificationService.createNotification(1L, "New Notification for RabbitMQ");

        assertNull(result); 
        verify(userRepository, never()).findById(anyLong()); // User lookup should NOT happen
        verify(notificationRepository, never()).save(any(Notification.class)); // Save should not be called
    }

    @Test
    void createNotification_whenUserDoesNotExist_andRabbitMQProfileNotActive_shouldThrowIllegalArgumentException() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{}); // RabbitMQ profile is NOT active
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            notificationService.createNotification(99L, "Notification for non-existent user");
        });
        assertEquals("User with id 99 not found. Cannot create notification.", exception.getMessage());
        verify(userRepository, times(1)).findById(99L); // findById is called
        verify(notificationRepository, never()).save(any(Notification.class)); // but save is not
    }

    @Test
    void createNotification_whenUserDoesNotExist_andRabbitMQProfileActive_shouldReturnNullAndNotThrowException() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"rabbitmq"}); // RabbitMQ profile IS active
        // No need to mock userRepository.findById, as it shouldn't be called if profile is active.

        Notification result = notificationService.createNotification(99L, "Notification for non-existent user with rabbit");
        
        assertNull(result); // Expect null because RabbitMQ profile is active, short-circuiting before user check for save
        verify(userRepository, never()).findById(anyLong()); // User lookup should NOT happen
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
        testNotification1.setRead(false); // Ensure it's unread
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
        testNotification2.setRead(true); // Ensure it's read
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
        verify(notificationRepository, times(1)).findById(99L);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAsRead_whenNotificationDoesNotBelongToUser_shouldReturnEmpty() {
        Notification otherUserNotification = new Notification(2L, "Other user's notification");
        otherUserNotification.setId(3L);
        otherUserNotification.setRead(false);
        when(notificationRepository.findById(3L)).thenReturn(Optional.of(otherUserNotification));

        Optional<Notification> result = notificationService.markAsRead(3L, 1L);

        assertFalse(result.isPresent());
        verify(notificationRepository, times(1)).findById(3L);
        verify(notificationRepository, never()).save(any(Notification.class));
    }
} 