package com.max.taskmanager.repository;

import com.max.taskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository; // @Repository здесь опционален, JpaRepository уже является компонентом

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> { // Указываем тип сущности и тип ID

    Optional<User> findByUsername(String username);

}