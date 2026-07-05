package io.github.ArtemV2007.listener;

import io.github.ArtemV2007.dto.UserEvent;
import io.github.ArtemV2007.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationListener {

    private final EmailService emailService;

    // Внедряем EmailService для отправки писем
    public KafkaNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Метод автоматически вызывается при поступлении нового сообщения в топик Kafka.
     * @param event Десериализованный объект события (содержит action и email)
     */
    @KafkaListener(
            topics = "user-events",
            groupId = "notification-group"
    )
    public void listen(UserEvent event) {
        System.out.println("Получено событие из Kafka: Действие = " + event.action() + ", Email = " + event.email());

        // Передаем данные в сервис для отправки email
        emailService.sendNotification(event.email(), event.action());
    }
}
