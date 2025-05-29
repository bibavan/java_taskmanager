package com.max.taskmanager.service;

import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import com.max.taskmanager.repository.TaskRepository;
import com.max.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Task createTask(Long userId, String title, String description, LocalDateTime targetDate) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found. Cannot create task."));

        Task task = new Task();
        task.setUserId(userId);
        task.setTitle(title);
        task.setDescription(description);
        task.setTargetDate(targetDate);
        task.setCreationDate(LocalDateTime.now());
        task.setStatus(TaskStatus.PENDING);
        task.setDeleted(false);
        return taskRepository.save(task);
    }

    @Override
    public Optional<Task> getTaskByIdAndUserId(Long taskId, Long userId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getUserId().equals(userId) && !task.isDeleted());
    }

    @Override
    public List<Task> getAllUserTasks(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found."));
        return taskRepository.findAllByUserIdAndDeletedFalse(userId);
    }

    @Override
    public List<Task> getPendingUserTasks(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found."));
        return taskRepository.findAllByUserIdAndStatusAndDeletedFalse(userId, TaskStatus.PENDING);
    }

    @Override
    public Optional<Task> deleteTask(Long taskId, Long userId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            if (task.getUserId().equals(userId) && !task.isDeleted()) {
                task.setDeleted(true);
                return Optional.of(taskRepository.save(task));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Task> updateTaskStatus(Long taskId, Long userId, TaskStatus status) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            if (task.getUserId().equals(userId) && !task.isDeleted()) {
                task.setStatus(status);
                return Optional.of(taskRepository.save(task));
            }
        }
        return Optional.empty();
    }
}