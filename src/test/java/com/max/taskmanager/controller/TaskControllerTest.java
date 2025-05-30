package com.max.taskmanager.controller;

import com.max.taskmanager.dto.CreateTaskRequest;
import com.max.taskmanager.dto.UpdateTaskStatusRequest;
import com.max.taskmanager.model.Task;
import com.max.taskmanager.model.TaskStatus;
import com.max.taskmanager.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    private final Long testUserId = 1L;
    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules(); // Good practice to register all modules

        task1 = new Task(testUserId, "Task 1", "Desc 1", LocalDateTime.now().plusDays(1));
        task1.setId(1L);
        task1.setCreationDate(LocalDateTime.now());
        task1.setStatus(TaskStatus.PENDING);

        task2 = new Task(testUserId, "Task 2", "Desc 2", LocalDateTime.now().plusDays(2));
        task2.setId(2L);
        task2.setCreationDate(LocalDateTime.now());
        task2.setStatus(TaskStatus.COMPLETED);
    }

    @Test
    void createTask_whenValidRequest_shouldReturnCreatedTask() throws Exception {
        CreateTaskRequest createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setTitle("New Task");
        createTaskRequest.setDescription("New Description");
        LocalDateTime targetDate = LocalDateTime.of(2025, 1, 1, 10, 0);
        createTaskRequest.setTargetDate(targetDate);

        Task createdTask = new Task(testUserId, "New Task", "New Description", targetDate);
        createdTask.setId(3L);
        createdTask.setCreationDate(LocalDateTime.now());
        createdTask.setStatus(TaskStatus.PENDING);

        when(taskService.createTask(eq(testUserId), eq("New Task"), eq("New Description"), eq(targetDate)))
                .thenReturn(createdTask);

        mockMvc.perform(post("/api/users/{userId}/tasks", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.title").value("New Task"));
    }

    @Test
    void createTask_whenServiceThrowsIllegalArgument_shouldReturnBadRequest() throws Exception {
        CreateTaskRequest createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setTitle("New Task");
        LocalDateTime targetDate = LocalDateTime.of(2025, 1, 1, 10, 0);
        createTaskRequest.setTargetDate(targetDate);

        Long nonExistentUserId = 99L;

        when(taskService.createTask(eq(nonExistentUserId), eq("New Task"), nullable(String.class), eq(targetDate)))
                .thenThrow(new IllegalArgumentException("User not found"));

        mockMvc.perform(post("/api/users/{userId}/tasks", nonExistentUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("User not found"));
    }


    @Test
    void getAllUserTasks_shouldReturnListOfTasks() throws Exception {
        when(taskService.getAllUserTasks(testUserId)).thenReturn(Arrays.asList(task1, task2));

        mockMvc.perform(get("/api/users/{userId}/tasks", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(task1.getId()))
                .andExpect(jsonPath("$[1].id").value(task2.getId()));
    }

    @Test
    void getPendingUserTasks_shouldReturnListOfPendingTasks() throws Exception {
        when(taskService.getPendingUserTasks(testUserId)).thenReturn(Collections.singletonList(task1));

        mockMvc.perform(get("/api/users/{userId}/tasks/pending", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(task1.getId()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getTaskById_whenTaskExists_shouldReturnTask() throws Exception {
        when(taskService.getTaskByIdAndUserId(task1.getId(), testUserId)).thenReturn(Optional.of(task1));

        mockMvc.perform(get("/api/users/{userId}/tasks/{taskId}", testUserId, task1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task1.getId()));
    }

    @Test
    void getTaskById_whenTaskNotFound_shouldReturnNotFound() throws Exception {
        Long nonExistentTaskId = 99L;
        when(taskService.getTaskByIdAndUserId(nonExistentTaskId, testUserId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{userId}/tasks/{taskId}", testUserId, nonExistentTaskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Task not found or access denied"));
    }

    @Test
    void deleteTask_whenTaskExists_shouldReturnNoContent() throws Exception {
        when(taskService.deleteTask(task1.getId(), testUserId)).thenReturn(Optional.of(task1));

        mockMvc.perform(delete("/api/users/{userId}/tasks/{taskId}", testUserId, task1.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_whenTaskNotFound_shouldReturnNotFound() throws Exception {
        Long nonExistentTaskId = 99L;
        when(taskService.deleteTask(nonExistentTaskId, testUserId)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/users/{userId}/tasks/{taskId}", testUserId, nonExistentTaskId))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Task not found or access denied for deletion"));
    }

    @Test
    void updateTaskStatus_whenValid_shouldReturnUpdatedTask() throws Exception {
        UpdateTaskStatusRequest statusRequest = new UpdateTaskStatusRequest();
        statusRequest.setStatus(TaskStatus.COMPLETED);

        Task updatedTask = new Task(testUserId, task1.getTitle(), task1.getDescription(), task1.getTargetDate());
        updatedTask.setId(task1.getId());
        updatedTask.setStatus(TaskStatus.COMPLETED);
        updatedTask.setCreationDate(task1.getCreationDate());

        when(taskService.updateTaskStatus(task1.getId(), testUserId, TaskStatus.COMPLETED))
            .thenReturn(Optional.of(updatedTask));

        mockMvc.perform(patch("/api/users/{userId}/tasks/{taskId}/status", testUserId, task1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
} 