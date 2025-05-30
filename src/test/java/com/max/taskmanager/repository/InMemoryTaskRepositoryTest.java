package com.max.taskmanager.repository;

import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import com.max.taskmanager.repository.impl.InMemoryTaskRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskRepositoryTest {

    private TaskRepository taskRepository;
    private final Long userId1 = 1L;
    private final Long userId2 = 2L;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepositoryImpl();
    }

    @Test
    void save_newTask_shouldAssignIdAndStore() {
        Task task = new Task(userId1, "Test Task", "Desc", LocalDateTime.now().plusDays(1));
        Task savedTask = taskRepository.save(task);

        assertNotNull(savedTask.getId());
        assertEquals(userId1, savedTask.getUserId());
        Optional<Task> found = taskRepository.findById(savedTask.getId());
        assertTrue(found.isPresent());
        assertEquals(savedTask, found.get());
    }

    @Test
    void save_existingTask_shouldUpdate() {
        Task task = taskRepository.save(new Task(userId1, "Original Title", "Desc", LocalDateTime.now().plusDays(1)));
        Long originalId = task.getId();

        task.setTitle("Updated Title");
        Task updatedTask = taskRepository.save(task);

        assertEquals(originalId, updatedTask.getId());
        assertEquals("Updated Title", updatedTask.getTitle());

        Optional<Task> found = taskRepository.findById(originalId);
        assertTrue(found.isPresent());
        assertEquals("Updated Title", found.get().getTitle());
    }


    @Test
    void findAllByUserIdAndDeletedFalse_shouldReturnCorrectTasks() {
        Task task1User1 = taskRepository.save(new Task(userId1, "U1T1", "D1", LocalDateTime.now()));
        Task task2User1 = taskRepository.save(new Task(userId1, "U1T2", "D2", LocalDateTime.now()));
        task2User1.setDeleted(true);
        taskRepository.save(task2User1);
        taskRepository.save(new Task(userId2, "U2T1", "D3", LocalDateTime.now()));

        List<Task> user1Tasks = taskRepository.findAllByUserIdAndDeletedFalse(userId1);
        assertEquals(1, user1Tasks.size());
        assertTrue(user1Tasks.contains(task1User1));
        assertFalse(user1Tasks.stream().anyMatch(t -> t.getId().equals(task2User1.getId())));

    }

    @Test
    void findAllByUserIdAndStatusAndDeletedFalse_shouldReturnCorrectTasks() {
        Task taskPending = new Task(userId1, "Pending", "D", LocalDateTime.now());
        taskPending.setStatus(TaskStatus.PENDING);
        taskRepository.save(taskPending);

        Task taskCompleted = new Task(userId1, "Completed", "D", LocalDateTime.now());
        taskCompleted.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(taskCompleted);

        Task taskDeletedPending = new Task(userId1, "Deleted Pending", "D", LocalDateTime.now());
        taskDeletedPending.setStatus(TaskStatus.PENDING);
        taskDeletedPending.setDeleted(true);
        taskRepository.save(taskDeletedPending);

        List<Task> pendingTasks = taskRepository.findAllByUserIdAndStatusAndDeletedFalse(userId1, TaskStatus.PENDING);
        assertEquals(1, pendingTasks.size());
        assertTrue(pendingTasks.contains(taskPending));

        List<Task> completedTasks = taskRepository.findAllByUserIdAndStatusAndDeletedFalse(userId1, TaskStatus.COMPLETED);
        assertEquals(1, completedTasks.size());
        assertTrue(completedTasks.contains(taskCompleted));
    }

    @Test
    void findById_whenTaskExists_shouldReturnTask() {
        Task task = taskRepository.save(new Task(userId1, "Find Me", "Desc", LocalDateTime.now()));
        Optional<Task> found = taskRepository.findById(task.getId());
        assertTrue(found.isPresent());
        assertEquals(task, found.get());
    }

    @Test
    void findById_whenTaskDoesNotExist_shouldReturnEmpty() {
        Optional<Task> found = taskRepository.findById(999L);
        assertFalse(found.isPresent());
    }
} 