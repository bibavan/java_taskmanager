package com.max.taskmanager.service;

import com.max.taskmanager.dto.TaskNotificationDTO;
import com.max.taskmanager.model.Notification;
import com.max.taskmanager.repository.NotificationRepository;
import com.max.taskmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@Profile("rabbitmq")
public class RabbitMQNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQNotificationListener.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired
    public RabbitMQNotificationListener(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.name}")
    public void handleTaskCreation(TaskNotificationDTO taskNotificationDTO) {
        logger.info("Received task notification: {}", taskNotificationDTO);

        if (!"TASK_CREATED".equals(taskNotificationDTO.getMessageType())) {
            logger.warn("Received notification with unknown message type: {}", taskNotificationDTO.getMessageType());
            return;
        }

        userRepository.findById(taskNotificationDTO.getUserId()).ifPresentOrElse(
            user -> {
                Notification notification = new Notification();
                notification.setUserId(taskNotificationDTO.getUserId());
                notification.setMessage("New task '" + taskNotificationDTO.getTitle() + "' has been created for you.");
                notification.setCreationDate(LocalDateTime.now());
                notification.setRead(false);
                notificationRepository.save(notification);
                logger.info("Notification created for user {} regarding task {}", taskNotificationDTO.getUserId(), taskNotificationDTO.getTaskId());
            },
            () -> logger.error("User with id {} not found. Cannot create notification for task {}.", taskNotificationDTO.getUserId(), taskNotificationDTO.getTaskId())
        );
    }
} 