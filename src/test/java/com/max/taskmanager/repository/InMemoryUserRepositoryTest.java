package com.max.taskmanager.repository;

import com.max.taskmanager.model.User;
import com.max.taskmanager.repository.impl.InMemoryUserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepositoryTest {

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepositoryImpl();
    }

    @Test
    void save_whenNewUser_shouldAssignIdAndStoreUser() {
        User newUser = new User(null, "testuser", "password");

        User savedUser = userRepository.save(newUser);

        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());

        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals(savedUser, foundUser.get());

        Optional<User> foundByUsername = userRepository.findByUsername("testuser");
        assertTrue(foundByUsername.isPresent());
        assertEquals(savedUser.getId(), foundByUsername.get().getId());
    }

    @Test
    void save_whenExistingUser_shouldUpdateUser() {
        User initialUser = userRepository.save(new User(null, "updater", "oldpass"));
        Long userId = initialUser.getId();

        User userToUpdate = new User(userId, "updater", "newpass");

        User updatedUser = userRepository.save(userToUpdate);

        assertEquals(userId, updatedUser.getId());
        assertEquals("newpass", updatedUser.getPassword());

        Optional<User> foundUser = userRepository.findById(userId);
        assertTrue(foundUser.isPresent());
        assertEquals("newpass", foundUser.get().getPassword());
    }

    @Test
    void save_duplicateUsername_findByUsernameShouldReturnCorrectOne() {
        User user1 = userRepository.save(new User(null, "duplicateuser", "pass1"));
        User user2 = new User(null, "duplicateuser", "pass2");

        Optional<User> found = userRepository.findByUsername("duplicateuser");
        assertTrue(found.isPresent());
        assertEquals(user1.getId(), found.get().getId());
    }

    @Test
    void findById_whenUserExists_shouldReturnUser() {
        User savedUser = userRepository.save(new User(null, "findme", "password"));

        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        assertTrue(foundUser.isPresent());
        assertEquals(savedUser, foundUser.get());
    }

    @Test
    void findById_whenUserDoesNotExist_shouldReturnEmptyOptional() {
        Optional<User> foundUser = userRepository.findById(999L);
        assertFalse(foundUser.isPresent());
    }

    @Test
    void findByUsername_whenUserExists_shouldReturnUser() {
        User savedUser = userRepository.save(new User(null, "user123", "password"));

        Optional<User> foundUser = userRepository.findByUsername("user123");

        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals("user123", foundUser.get().getUsername());
    }

    @Test
    void findByUsername_whenUserDoesNotExist_shouldReturnEmptyOptional() {
        Optional<User> foundUser = userRepository.findByUsername("nonexistentuser");
        assertFalse(foundUser.isPresent());
    }
} 