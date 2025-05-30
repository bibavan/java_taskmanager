package com.max.taskmanager.service;

import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import com.max.taskmanager.model.User;
import com.max.taskmanager.repository.TaskRepository;
import com.max.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User testUser;
    private Task testTask1;
    private Task testTask2;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "testuser", "password");
        testTask1 = new Task(1L, "Task 1", "Desc 1", LocalDateTime.now().plusDays(1));
        testTask1.setId(1L);
        testTask1.setStatus(TaskStatus.PENDING);
        testTask1.setDeleted(false);

        testTask2 = new Task(1L, "Task 2", "Desc 2", LocalDateTime.now().plusDays(2));
        testTask2.setId(2L);
        testTask2.setStatus(TaskStatus.COMPLETED);
        testTask2.setDeleted(false);
    }

    @Test
    void createTask_whenUserExists_shouldSaveAndReturnTask() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task taskToSave = invocation.getArgument(0);
            if (taskToSave.getId() == null && "New Task".equals(taskToSave.getTitle())) {
                taskToSave.setId(3L);
            }
            return taskToSave;
        });

        Task result = taskService.createTask(1L, "New Task", "New Desc", LocalDateTime.now().plusDays(5));

        assertNotNull(result);
        assertEquals("New Task", result.getTitle());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        assertFalse(result.isDeleted());
        assertNotNull(result.getCreationDate());
        verify(userRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void createTask_whenUserDoesNotExist_shouldThrowIllegalArgumentException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.createTask(99L, "Task for non-existent user", "Desc", LocalDateTime.now());
        });
        assertEquals("User with id 99 not found. Cannot create task.", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void getAllUserTasks_whenUserExists_shouldReturnNonDeletedTasks() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.findAllByUserIdAndDeletedFalse(1L)).thenReturn(Arrays.asList(testTask1, testTask2));

        List<Task> result = taskService.getAllUserTasks(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testTask1));
        assertTrue(result.contains(testTask2));
        verify(userRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).findAllByUserIdAndDeletedFalse(1L);
    }

    @Test
    void getAllUserTasks_whenUserDoesNotExist_shouldThrowIllegalArgumentException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.getAllUserTasks(99L);
        });
        assertEquals("User with id 99 not found.", exception.getMessage());
        verify(taskRepository, never()).findAllByUserIdAndDeletedFalse(anyLong());
    }


    @Test
    void getPendingUserTasks_whenUserExists_shouldReturnPendingNonDeletedTasks() {
        Task pendingTask = new Task(1L, "Pending Task", "Desc Pending", LocalDateTime.now().plusDays(3));
        pendingTask.setId(3L);
        pendingTask.setStatus(TaskStatus.PENDING);
        pendingTask.setDeleted(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.findAllByUserIdAndStatusAndDeletedFalse(1L, TaskStatus.PENDING))
                .thenReturn(Collections.singletonList(pendingTask));

        List<Task> result = taskService.getPendingUserTasks(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(pendingTask, result.get(0));
        verify(userRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).findAllByUserIdAndStatusAndDeletedFalse(1L, TaskStatus.PENDING);
    }

    @Test
    void deleteTask_whenTaskExistsAndBelongsToUser_shouldMarkAsDeleted() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask1);

        Optional<Task> result = taskService.deleteTask(1L, 1L);

        assertTrue(result.isPresent());
        assertTrue(result.get().isDeleted());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(testTask1);
    }

    @Test
    void deleteTask_whenTaskNotFound_shouldReturnEmpty() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Task> result = taskService.deleteTask(99L, 1L);

        assertFalse(result.isPresent());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTask_whenTaskDoesNotBelongToUser_shouldReturnEmpty() {
        Task otherUserTask = new Task(2L, "Other User Task", "Desc", LocalDateTime.now());
        otherUserTask.setId(5L);

        when(taskRepository.findById(5L)).thenReturn(Optional.of(otherUserTask));

        Optional<Task> result = taskService.deleteTask(5L, 1L);

        assertFalse(result.isPresent());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTaskStatus_whenTaskExistsAndBelongsToUser_shouldUpdateStatus() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask1));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Task> result = taskService.updateTaskStatus(1L, 1L, TaskStatus.COMPLETED);

        assertTrue(result.isPresent());
        assertEquals(TaskStatus.COMPLETED, result.get().getStatus());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(testTask1);
    }
} 