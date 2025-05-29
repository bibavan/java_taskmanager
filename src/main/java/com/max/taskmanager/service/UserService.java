package com.max.taskmanager.service;

import com.max.taskmanager.model.User;
import java.util.Optional;

public interface UserService {
    User registerUser(User user);
    Optional<User> loginUser(String username, String password);
}