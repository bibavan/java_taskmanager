package com.max.taskmanager.repository;

import com.max.taskmanager.model.User;
import org.springframework.stereotype.Repository; // Важно для Spring DI

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository // Помечаем класс как компонент Spring (бин репозитория)
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<String, Long> usernameToIdMap = new ConcurrentHashMap<>(); // для быстрого поиска по username
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            if (usernameToIdMap.containsKey(user.getUsername())) {
                // В реальном приложении здесь лучше бросать кастомное исключение
                throw new IllegalArgumentException("User with username " + user.getUsername() + " already exists.");
            }
            user.setId(idGenerator.incrementAndGet());
        }
        users.put(user.getId(), user);
        usernameToIdMap.put(user.getUsername(), user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Long userId = usernameToIdMap.get(username);
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(userId));
    }
}