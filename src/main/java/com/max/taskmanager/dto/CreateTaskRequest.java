package com.max.taskmanager.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateTaskRequest {
    private String title;
    private String description;
    private LocalDateTime targetDate;
} 