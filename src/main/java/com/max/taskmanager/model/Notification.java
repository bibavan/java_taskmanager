package com.max.taskmanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Notification {
    private Long id;
    private Long userId;
    private String message;
    private LocalDateTime creationDate;
    private boolean read = false;

    public Notification(Long id, Long userId, String message) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.creationDate = LocalDateTime.now();
        this.read = false;
    }
}