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

        // userRepository.findByUsername is called by the save method in InMemoryUserRepository
        // to check for duplicates. For this test, we assume the service layer itself
        // might not call it directly before calling save if the repository handles the check.
        // However, the provided UserServiceImpl doesn't call findByUsername before save,
        // it relies on the repository's save method to throw an exception if a duplicate occurs.
        // The provided UserServiceImpl does not call findByUsername before save.
        // The InMemoryUserRepository.save() method DOES check for duplicates.
        // So, to test the successful path of userService.registerUser, we need to mock what the repository's save method would do.
        // For a unique user, the save method is called.
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(userToRegister);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).save(userToRegister);
    }

    @Test
    void registerUser_whenUsernameAlreadyExists_shouldThrowIllegalArgumentException() {
        User userToRegister = new User(null, "testuser", "newpassword");

        //This mock simulates the scenario where the repository's save method detects a duplicate username and throws an exception.
        when(userRepository.save(any(User.class)))
                .thenThrow(new IllegalArgumentException("User with username testuser already exists."));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(userToRegister);
        });
        assertEquals("User with username testuser already exists.", exception.getMessage());
        verify(userRepository, times(1)).save(userToRegister); // save is called, but it throws the exception
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