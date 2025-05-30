package com.max.taskmanager.repository.impl;

import com.max.taskmanager.model.User;
import com.max.taskmanager.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@Profile("in-memory")
public class InMemoryUserRepositoryImpl implements UserRepository {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    // Custom methods from UserRepository interface
    @Override
    public Optional<User> findByUsername(String username) {
        return userStore.values().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    // Methods from CrudRepository / ListCrudRepository
    @Override
    public <S extends User> S save(S entity) {
        if (entity.getId() == null) {
            if (findByUsername(entity.getUsername()).isPresent()) {
                throw new IllegalArgumentException("User with username " + entity.getUsername() + " already exists.");
            }
            entity.setId(idGenerator.incrementAndGet());
        } else {
            if (!userStore.containsKey(entity.getId())) {
                throw new EntityNotFoundException("User with id " + entity.getId() + " not found for update.");
            }
        }
        userStore.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public <S extends User> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        entities.forEach(entity -> result.add(save(entity)));
        return result;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userStore.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return userStore.containsKey(id);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(userStore.values());
    }

    @Override
    public List<User> findAllById(Iterable<Long> ids) {
        List<User> result = new ArrayList<>();
        ids.forEach(id -> {
            User user = userStore.get(id);
            if (user != null) {
                result.add(user);
            }
        });
        return result;
    }

    @Override
    public long count() {
        return userStore.size();
    }

    @Override
    public void deleteById(Long id) {
        userStore.remove(id);
    }

    @Override
    public void delete(User entity) {
        if (entity != null) {
            userStore.remove(entity.getId());
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends User> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        userStore.clear();
    }

    // Methods from PagingAndSortingRepository / ListPagingAndSortingRepository
    @Override
    public List<User> findAll(Sort sort) {
        throw new UnsupportedOperationException("findAll(Sort) not fully supported for in-memory repository");
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("findAll(Pageable) not fully supported for in-memory repository");
    }

    // Methods from QueryByExampleExecutor (JpaRepository redeclares some to return List<S>)
    @Override
    public <S extends User> Optional<S> findOne(Example<S> example) {
        throw new UnsupportedOperationException("findOne(Example) not supported for in-memory repository");
    }

    @Override
    public <S extends User> List<S> findAll(Example<S> example) { 
        throw new UnsupportedOperationException("findAll(Example) not supported for in-memory repository");
    }

    @Override
    public <S extends User> List<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException("findAll(Example, Sort) not supported for in-memory repository");
    }

    @Override
    public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException("findAll(Example, Pageable) not supported for in-memory repository");
    }

    @Override
    public <S extends User> long count(Example<S> example) {
        throw new UnsupportedOperationException("count(Example) not supported for in-memory repository");
    }

    @Override
    public <S extends User> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException("exists(Example) not supported for in-memory repository");
    }
    
    @Override
    public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException("findBy(Example, Function) not supported for in-memory repository");
    }

    // Methods specific to JpaRepository
    @Override
    public void flush() {
        // No-op for in-memory.
    }

    @Override
    public <S extends User> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<User> entities) {
         deleteAll(entities);
    }
    
    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
         deleteAllById(ids);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    @Deprecated
    public User getOne(Long id) {
        return findById(id).orElse(null);
    }

    @Override
    public User getById(Long id) {
         return findById(id).orElseThrow(() -> new EntityNotFoundException("Unable to find User with id " + id));
    }
    
    @Override
    public User getReferenceById(Long id) {
         return findById(id).orElseThrow(() -> new EntityNotFoundException("Unable to find User with id " + id));
    }
} 