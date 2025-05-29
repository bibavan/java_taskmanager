package com.max.taskmanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Task {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private LocalDateTime creationDate;
    private LocalDateTime targetDate;
    private TaskStatus status;
    private boolean deleted = false;

    public Task(Long id, Long userId, String title, String description, LocalDateTime targetDate) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.creationDate = LocalDateTime.now();
        this.targetDate = targetDate;
        this.status = TaskStatus.PENDING;
        this.deleted = false;
    }
}