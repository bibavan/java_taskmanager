package com.max.taskmanager.dto;

import com.max.taskmanager.model.TaskStatus;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {
    private TaskStatus status;
} 