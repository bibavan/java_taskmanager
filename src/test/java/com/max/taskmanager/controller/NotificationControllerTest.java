package com.max.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.max.taskmanager.model.Notification;
import com.max.taskmanager.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private final Long testUserId = 1L;
    private Notification notification1;
    private Notification notification2;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();

        notification1 = new Notification(testUserId, "Notification 1");
        notification1.setId(1L);
        notification1.setCreationDate(LocalDateTime.now().minusHours(1));
        notification1.setRead(false);

        notification2 = new Notification(testUserId, "Notification 2");
        notification2.setId(2L);
        notification2.setCreationDate(LocalDateTime.now().minusHours(2));
        notification2.setRead(true);
    }

    @Test
    void createNotification_whenValid_shouldReturnCreatedNotification() throws Exception {
        String message = "Test Notification Message";
        Notification createdNotification = new Notification(testUserId, message);
        createdNotification.setId(3L);
        createdNotification.setCreationDate(LocalDateTime.now());
        createdNotification.setRead(false);

        when(notificationService.createNotification(eq(testUserId), eq(message)))
                .thenReturn(createdNotification);

        mockMvc.perform(post("/api/users/{userId}/notifications", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Notification(testUserId, message))))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.read").value(false));
    }

    @Test
    void getAllUserNotifications_shouldReturnListOfNotifications() throws Exception {
        when(notificationService.getAllUserNotifications(testUserId)).thenReturn(Arrays.asList(notification1, notification2));

        mockMvc.perform(get("/api/users/{userId}/notifications", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(notification1.getId()));
    }

    @Test
    void getPendingUserNotifications_shouldReturnListOfUnreadNotifications() throws Exception {
        when(notificationService.getPendingUserNotifications(testUserId)).thenReturn(Collections.singletonList(notification1));

        mockMvc.perform(get("/api/users/{userId}/notifications/pending", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(notification1.getId()))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void markNotificationAsRead_whenNotificationExists_shouldReturnUpdatedNotification() throws Exception {
        Notification updatedNotification = new Notification(testUserId, notification1.getMessage());
        updatedNotification.setId(notification1.getId());
        updatedNotification.setRead(true);
        updatedNotification.setCreationDate(notification1.getCreationDate());

        when(notificationService.markAsRead(notification1.getId(), testUserId)).thenReturn(Optional.of(updatedNotification));

        mockMvc.perform(patch("/api/users/{userId}/notifications/{notificationId}/read", testUserId, notification1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification1.getId()))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markNotificationAsRead_whenNotificationNotFound_shouldReturnNotFound() throws Exception {
        Long nonExistentNotificationId = 99L;
        when(notificationService.markAsRead(nonExistentNotificationId, testUserId)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/users/{userId}/notifications/{notificationId}/read", testUserId, nonExistentNotificationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Notification not found or access denied"));
    }
} 