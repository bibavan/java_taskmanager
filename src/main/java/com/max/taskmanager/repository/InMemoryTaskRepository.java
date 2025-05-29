package com.max.taskmanager.repository;

import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryTaskRepository implements TaskRepository {
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Task save(Task task) {
        if (task.getId() == null) {
            task.setId(idGenerator.incrementAndGet());
        }
        tasks.put(task.getId(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(Long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findAllByUserId(Long userId) {
        return tasks.values().stream()
                .filter(task -> task.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findAllByUserIdAndDeletedFalse(Long userId) {
        return tasks.values().stream()
                .filter(task -> task.getUserId().equals(userId) && !task.isDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findAllByUserIdAndStatusAndDeletedFalse(Long userId, TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.getUserId().equals(userId) && task.getStatus() == status && !task.isDeleted())
                .collect(Collectors.toList());
    }
}