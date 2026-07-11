package io.github.ArtemV2007.listener;

import io.github.ArtemV2007.dto.UserEvent;
import io.github.ArtemV2007.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationListener {

    // Инициализируем логгер
    private static final Logger logger = LoggerFactory.getLogger(KafkaNotificationListener.class);

    private final EmailService emailService;

    public KafkaNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "user-events", groupId = "notification-group")
    public void listen(UserEvent event) {
        // ИСПРАВЛЕНО: заменено System.out на logger.info
        logger.info("Получено событие из Kafka: Действие = {}, Email = {}", event.action(), event.email());

        emailService.sendNotification(event.email(), event.action());
    }
}
