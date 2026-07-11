package io.github.ArtemV2007.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    // Инициализируем логгер
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendNotification(String email, String action) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom("noreply@yourdomain.com");

        if ("CREATE".equalsIgnoreCase(action)) {
            message.setSubject("Добро пожаловать!");
            message.setText("Здравствуйте! Ваш acкayнт нa caйтe вaш caйт был yспeшнo coздaн.");
        } else if ("DELETE".equalsIgnoreCase(action)) {
            message.setSubject("Аккаунт удален");
            message.setText("Здравствуйте! Ваш аккаунт был удалён.");
        } else {
            // ИСПРАВЛЕНО: заменено System.out на logger.warn
            logger.warn("Неизвестное действие для отправки email: {}", action);
            return;
        }

        mailSender.send(message);
    }
}
