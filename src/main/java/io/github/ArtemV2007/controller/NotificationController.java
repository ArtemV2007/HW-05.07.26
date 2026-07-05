package io.github.ArtemV2007.controller;

import io.github.ArtemV2007.dto.UserEvent;
import io.github.ArtemV2007.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final EmailService emailService;

    // Внедряем сервис отправки почты через конструктор
    public NotificationController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * POST эндпоинт для прямой отправки уведомлений на почту в обход Kafka.
     * Пример тела запроса JSON:
     * {
     *   "action": "CREATE",
     *   "email": "user@example.com"
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<Void> sendDirectNotification(@Valid @RequestBody UserEvent event) {
        // Логируем факт вызова через API
        System.out.println("Вызвано прямое API уведомлений: Действие = " + event.action() + ", Email = " + event.email());

        // Вызываем логику отправки письма
        emailService.sendNotification(event.email(), event.action());

        // Возвращаем статус 200 OK без тела ответа
        return ResponseEntity.ok().build();
    }
}
