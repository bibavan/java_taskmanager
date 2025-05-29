package com.max.taskmanager.controller;

import com.max.taskmanager.dto.CreateTaskRequest;
import com.max.taskmanager.dto.UpdateTaskStatusRequest;
import com.max.taskmanager.model.Task;
import com.max.taskmanager.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/tasks")
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@PathVariable Long userId, @RequestBody CreateTaskRequest taskRequest) {
        try {
            Task createdTask = taskService.createTask(
                    userId,
                    taskRequest.getTitle(),
                    taskRequest.getDescription(),
                    taskRequest.getTargetDate()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllUserTasks(@PathVariable Long userId) {
        try {
            List<Task> tasks = taskService.getAllUserTasks(userId);
            return ResponseEntity.ok(tasks);
        } catch (IllegalArgumentException e) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Task>> getPendingUserTasks(@PathVariable Long userId) {
         try {
            List<Task> tasks = taskService.getPendingUserTasks(userId);
            return ResponseEntity.ok(tasks);
        } catch (IllegalArgumentException e) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long userId, @PathVariable Long taskId) {
        Task task = taskService.getTaskByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found or access denied"));
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long userId, @PathVariable Long taskId) {
        taskService.deleteTask(taskId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found or access denied for deletion"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<Task> updateTaskStatus(
            @PathVariable Long userId,
            @PathVariable Long taskId,
            @RequestBody UpdateTaskStatusRequest statusRequest) {
        Task updatedTask = taskService.updateTaskStatus(taskId, userId, statusRequest.getStatus())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found or access denied for status update"));
        return ResponseEntity.ok(updatedTask);
    }
} 