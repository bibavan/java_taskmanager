package com.max.taskmanager.repository;

import com.max.taskmanager.model.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    // В реальном приложении здесь были бы методы для получения всех пользователей, но для логина это не нужно
}