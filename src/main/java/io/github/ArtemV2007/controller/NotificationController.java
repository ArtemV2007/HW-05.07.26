package io.github.ArtemV2007.controller;

import io.github.ArtemV2007.dto.UserEvent;
import io.github.ArtemV2007.service.EmailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    // Инициализируем логгер
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final EmailService emailService;

    public NotificationController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendDirectNotification(@Valid @RequestBody UserEvent event) {
        // ИСПРАВЛЕНО: заменено System.out на logger.info
        logger.info("Вызвано прямое API уведомлений: Действие = {}, Email = {}", event.action(), event.email());

        emailService.sendNotification(event.email(), event.action());
        return ResponseEntity.ok().build();
    }
}
