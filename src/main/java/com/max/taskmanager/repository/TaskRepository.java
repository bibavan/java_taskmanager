package com.max.taskmanager.repository;

import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(Long id);
    List<Task> findAllByUserId(Long userId); // Все задачи пользователя (включая удаленные, если не фильтровать)
    List<Task> findAllByUserIdAndDeletedFalse(Long userId); // Все НЕ удаленные задачи пользователя
    List<Task> findAllByUserIdAndStatusAndDeletedFalse(Long userId, TaskStatus status); // Все НЕ удаленные задачи пользователя с определенным статусом
    // Метод delete не нужен, т.к. удаление мягкое через поле 'deleted'
}