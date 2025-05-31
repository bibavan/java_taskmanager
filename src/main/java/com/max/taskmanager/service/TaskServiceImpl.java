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
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    private TaskService self;

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

    @Autowired
    public void setSelf(@Lazy TaskService self) {
        this.self = self;
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
        @CachePut(value = "tasks", key = "#taskId + '_' + #userId")
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

    @Override
    @CacheEvict(value = "tasks", key = "#taskId + '_' + #userId")
    public void evictTaskCacheById(Long taskId, Long userId) {
        logger.debug("Evicting task from cache. TaskId: {}, UserId: {}", taskId, userId);
        // Method body is empty as @CacheEvict handles the logic
    }

    @Scheduled(fixedRate = 15000) // Set to 15 seconds
    public void checkAndProcessOverdueTasks() {
        logger.info("Checking for overdue tasks...");
        List<Task> pendingTasks = taskRepository.findAllByStatusAndDeletedFalse(TaskStatus.PENDING);

        LocalDateTime now = LocalDateTime.now();
        int overdueCount = 0;

        for (Task task : pendingTasks) {
            if (task.getTargetDate() != null && task.getTargetDate().isBefore(now)) {
                logger.info("Task ID: {} (User ID: {}) is overdue. Current status: {}. Target date: {}", 
                            task.getId(), task.getUserId(), task.getStatus(), task.getTargetDate());
                task.setStatus(TaskStatus.OVERDUE);
                taskRepository.save(task);
                // Evict from cache after updating status using self-injected proxy
                if (self != null) {
                    self.evictTaskCacheById(task.getId(), task.getUserId()); 
                } else {
                    logger.warn("Self-injected TaskService is null. Cache eviction might not work as expected for task ID: {}", task.getId());
                }
                overdueCount++;
                logger.info("Task ID: {} status updated to OVERDUE.", task.getId());
            }
        }
        if (overdueCount > 0) {
            logger.info("Processed {} overdue tasks.", overdueCount);
        } else {
            logger.info("No overdue tasks found.");
        }
    }
    
}