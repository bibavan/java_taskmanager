package com.max.taskmanager.service;

import com.max.taskmanager.model.User;
import com.max.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerUser_whenUsernameIsUnique_shouldSaveAndReturnUser() {
        User userToRegister = new User(null, "testuser", "password");
        User savedUser = new User(1L, "testuser", "password");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(userToRegister);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, times(1)).save(userToRegister);
    }

    @Test
    void registerUser_whenUsernameAlreadyExists_shouldThrowIllegalArgumentException() {
        User userToRegister = new User(null, "existinguser", "newpassword");
        User existingUser = new User(1L, "existinguser", "oldpassword");

        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existingUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(userToRegister);
        });
        assertEquals("User with username existinguser already exists.", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("existinguser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginUser_whenCredentialsAreValid_shouldReturnUser() {
        User storedUser = new User(1L, "testuser", "password");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(storedUser));

        Optional<User> result = userService.loginUser("testuser", "password");

        assertTrue(result.isPresent());
        assertEquals(storedUser, result.get());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void loginUser_whenPasswordIsInvalid_shouldReturnEmptyOptional() {
        User storedUser = new User(1L, "testuser", "password");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(storedUser));

        Optional<User> result = userService.loginUser("testuser", "wrongpassword");

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void loginUser_whenUserNotFound_shouldReturnEmptyOptional() {
        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        Optional<User> result = userService.loginUser("unknownuser", "password");

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByUsername("unknownuser");
    }
} 