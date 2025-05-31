package com.max.taskmanager.service;

import com.max.taskmanager.dto.TaskNotificationDTO;
import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import com.max.taskmanager.model.User;
import com.max.taskmanager.repository.TaskRepository;
import com.max.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private TaskServiceImpl taskService;

    private User testUser;
    private Task testTask1;
    private Task testTask2;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "testuser", "password");
        taskService = new TaskServiceImpl(taskRepository, userRepository, Optional.of(rabbitTemplate));
        ReflectionTestUtils.setField(taskService, "exchangeName", "test.exchange");
        ReflectionTestUtils.setField(taskService, "routingKey", "test.routing.key");

        LocalDateTime commonTimeForSetup = LocalDateTime.now();
        testTask1 = new Task(testUser.getId(), "Task 1", "Desc 1", commonTimeForSetup.plusDays(1));
        testTask1.setId(1L);
        testTask1.setStatus(TaskStatus.PENDING);
        testTask1.setDeleted(false);

        testTask2 = new Task(testUser.getId(), "Task 2", "Desc 2", commonTimeForSetup.plusDays(2));
        testTask2.setId(2L);
        testTask2.setStatus(TaskStatus.COMPLETED);
        testTask2.setDeleted(false);
    }

    @Test
    void createTask_whenUserExists_shouldSaveAndReturnTask_andSendMessageToRabbitMQ() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        LocalDateTime consistentTargetDate = LocalDateTime.now().plusDays(5);
        LocalDateTime consistentCreationDate = LocalDateTime.now();

        Task savedTaskOutput = new Task(1L, "New Task", "New Desc", consistentTargetDate);
        savedTaskOutput.setId(3L);
        savedTaskOutput.setCreationDate(consistentCreationDate);
        savedTaskOutput.setStatus(TaskStatus.PENDING);
        savedTaskOutput.setDeleted(false);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task taskToSave = invocation.getArgument(0);
            taskToSave.setId(savedTaskOutput.getId());
            savedTaskOutput.setCreationDate(taskToSave.getCreationDate());
            savedTaskOutput.setTargetDate(taskToSave.getTargetDate());
            return taskToSave;
        });

        Task result = taskService.createTask(1L, "New Task", "New Desc", consistentTargetDate);

        assertNotNull(result);
        assertEquals(savedTaskOutput.getId(), result.getId());
        assertEquals("New Task", result.getTitle());
        assertEquals("New Desc", result.getDescription());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        assertFalse(result.isDeleted());
        assertNotNull(result.getCreationDate());
        assertEquals(consistentTargetDate, result.getTargetDate());
        verify(userRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));

        ArgumentCaptor<TaskNotificationDTO> dtoCaptor = ArgumentCaptor.forClass(TaskNotificationDTO.class);
        verify(rabbitTemplate, times(1)).convertAndSend(eq("test.exchange"), eq("test.routing.key"), dtoCaptor.capture());
        TaskNotificationDTO sentDTO = dtoCaptor.getValue();
        assertEquals(result.getId(), sentDTO.getTaskId());
        assertEquals(result.getUserId(), sentDTO.getUserId());
        assertEquals(result.getTitle(), sentDTO.getTitle());
        assertEquals(result.getDescription(), sentDTO.getDescription());
        assertEquals(result.getCreationDate(), sentDTO.getCreationDate());
        assertEquals(consistentTargetDate, sentDTO.getTargetDate());
        assertEquals("TASK_CREATED", sentDTO.getMessageType());
    }

    @Test
    void createTask_whenUserExists_andRabbitMQProfileNotActive_shouldSaveAndReturnTask_andNotSendMessage() {
        TaskServiceImpl taskServiceWithoutRabbit = new TaskServiceImpl(taskRepository, userRepository, Optional.empty());
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        LocalDateTime consistentTargetDate = LocalDateTime.now().plusDays(5);

        Task savedTask = new Task(1L, "New Task", "New Desc", consistentTargetDate);
        savedTask.setId(4L);
        savedTask.setStatus(TaskStatus.PENDING);
        savedTask.setDeleted(false);
        savedTask.setTargetDate(consistentTargetDate);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task taskToSave = invocation.getArgument(0);
            taskToSave.setId(savedTask.getId());
            return taskToSave;
        });

        Task result = taskServiceWithoutRabbit.createTask(1L, "New Task", "New Desc", consistentTargetDate);

        assertNotNull(result);
        assertEquals(consistentTargetDate, result.getTargetDate());
        assertNotNull(result.getCreationDate());
        verify(userRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(TaskNotificationDTO.class));
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
        Task pendingTask = new Task(testUser.getId(), "Pending Task", "Desc Pending", LocalDateTime.now().plusDays(3));
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