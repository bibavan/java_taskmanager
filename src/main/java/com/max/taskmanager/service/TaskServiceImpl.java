package com.max.taskmanager.service;

import com.max.taskmanager.dto.TaskNotificationDTO;
import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import com.max.taskmanager.repository.TaskRepository;
import com.max.taskmanager.repository.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.name:#{null}}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing.key:#{null}}")
    private String routingKey;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository, Optional<RabbitTemplate> rabbitTemplateOpt) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplateOpt.orElse(null);
    }

    @Override
    @CachePut(value = "tasks", key = "#result.id + '_' + #result.userId")
    @CacheEvict(value = {"userTasks", "pendingUserTasks"}, allEntries = true)
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
        Task savedTask = taskRepository.save(task);

        if (rabbitTemplate != null && exchangeName != null && routingKey != null) {
            TaskNotificationDTO notificationDTO = new TaskNotificationDTO(
                savedTask.getId(),
                savedTask.getUserId(),
                savedTask.getTitle(),
                savedTask.getDescription(),
                savedTask.getCreationDate(),
                savedTask.getTargetDate(),
                savedTask.getStatus(),
                "TASK_CREATED"
            );
            rabbitTemplate.convertAndSend(exchangeName, routingKey, notificationDTO);
        }

        return savedTask;
    }

    @Override
    @Cacheable(value = "tasks", key = "#taskId + '_' + #userId", unless = "#result == null")
    public Optional<Task> getTaskByIdAndUserId(Long taskId, Long userId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getUserId().equals(userId) && !task.isDeleted());
    }

    @Override
    @Cacheable(value = "userTasks", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<Task> getAllUserTasks(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found."));
        return taskRepository.findAllByUserIdAndDeletedFalse(userId);
    }

    @Override
    @Cacheable(value = "pendingUserTasks", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<Task> getPendingUserTasks(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found."));
        return taskRepository.findAllByUserIdAndStatusAndDeletedFalse(userId, TaskStatus.PENDING);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "tasks", key = "#taskId + '_' + #userId"),
        @CacheEvict(value = {"userTasks", "pendingUserTasks"}, allEntries = true)
    })
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
    @Caching(put = {
        @CachePut(value = "tasks", key = "#taskId + '_' + #userId", unless = "#result == null || !#result.present")
    }, evict = {
        @CacheEvict(value = {"userTasks", "pendingUserTasks"}, allEntries = true)
    })
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