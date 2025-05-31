package com.max.taskmanager.service;

import com.max.taskmanager.dto.TaskNotificationDTO;
import com.max.taskmanager.model.Notification;
import com.max.taskmanager.model.TaskStatus;
import com.max.taskmanager.model.User;
import com.max.taskmanager.repository.NotificationRepository;
import com.max.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class) // Using MockitoExtension as this is not a full SpringBootTest for a listener component usually
class RabbitMQNotificationListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RabbitMQNotificationListener rabbitMQNotificationListener;

    @Test
    void handleTaskCreation_whenUserExistsAndMessageTypeIsCorrect_shouldCreateNotification() {
        TaskNotificationDTO dto = new TaskNotificationDTO(
                1L, 10L, "Test Task", "Test Description",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                TaskStatus.PENDING, "TASK_CREATED"
        );
        User mockUser = new User(10L, "testUser", "password");

        when(userRepository.findById(10L)).thenReturn(Optional.of(mockUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rabbitMQNotificationListener.handleTaskCreation(dto);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();

        assertEquals(dto.getUserId(), savedNotification.getUserId());
        assertEquals("New task '" + dto.getTitle() + "' has been created for you.", savedNotification.getMessage());
        assertFalse(savedNotification.isRead());
        assertNotNull(savedNotification.getCreationDate());

        verify(userRepository, times(1)).findById(10L);
    }

    @Test
    void handleTaskCreation_whenUserDoesNotExist_shouldNotCreateNotificationAndLogError() {
        TaskNotificationDTO dto = new TaskNotificationDTO(
                1L, 99L, "Test Task No User", "Test Description",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                TaskStatus.PENDING, "TASK_CREATED"
        );

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        rabbitMQNotificationListener.handleTaskCreation(dto);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(userRepository, times(1)).findById(99L);
        // Add logger verification if SLF4J is testable or use a spy if needed
    }

    @Test
    void handleTaskCreation_whenMessageTypeIsIncorrect_shouldNotCreateNotificationAndLogWarning() {
        TaskNotificationDTO dto = new TaskNotificationDTO(
                1L, 10L, "Test Task Wrong Type", "Test Description",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                TaskStatus.PENDING, "UNKNOWN_MESSAGE_TYPE"
        );
        // No need to mock userRepository or notificationRepository as they shouldn't be called if type is wrong

        rabbitMQNotificationListener.handleTaskCreation(dto);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(userRepository, never()).findById(anyLong());
        // Add logger verification if SLF4J is testable or use a spy if needed
    }
} 