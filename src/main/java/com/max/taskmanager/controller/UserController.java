package com.max.taskmanager.controller;

import com.max.taskmanager.dto.UserLoginRequest;
import com.max.taskmanager.dto.UserRegistrationRequest;
import com.max.taskmanager.model.User;
import com.max.taskmanager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody UserRegistrationRequest registrationRequest) {
        try {
            User newUser = new User();
            newUser.setUsername(registrationRequest.getUsername());
            newUser.setPassword(registrationRequest.getPassword());
            User registeredUser = userService.registerUser(newUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<User> loginUser(@RequestBody UserLoginRequest loginRequest) {
        User user = userService.loginUser(loginRequest.getUsername(), loginRequest.getPassword())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return ResponseEntity.ok(user);
    }
} 