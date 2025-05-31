package com.max.taskmanager.dto;

import com.max.taskmanager.model.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskNotificationDTO {
    private Long taskId;
    private Long userId;
    private String title;
    private String description;
    private LocalDateTime creationDate;
    private LocalDateTime targetDate;
    private TaskStatus status;
    private String messageType; // e.g., "TASK_CREATED", "TASK_UPDATED"
} 