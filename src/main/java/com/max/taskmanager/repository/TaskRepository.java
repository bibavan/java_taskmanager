package com.max.taskmanager.repository;

import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByUserIdAndDeletedFalse(Long userId);

    List<Task> findAllByUserIdAndStatusAndDeletedFalse(Long userId, TaskStatus status);

}