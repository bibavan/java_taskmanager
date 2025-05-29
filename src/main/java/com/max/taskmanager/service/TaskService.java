package com.max.taskmanager.service;

import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskService {
    Task createTask(Long userId, String title, String description, LocalDateTime targetDate);
    Optional<Task> getTaskByIdAndUserId(Long taskId, Long userId); // Получить задачу, если она принадлежит пользователю и не удалена
    List<Task> getAllUserTasks(Long userId); // Все НЕ удаленные задачи пользователя
    List<Task> getPendingUserTasks(Long userId); // Все НЕ удаленные PENDING задачи пользователя
    Optional<Task> deleteTask(Long taskId, Long userId); // Мягкое удаление
    Optional<Task> updateTaskStatus(Long taskId, Long userId, TaskStatus status); // Для изменения статуса
}