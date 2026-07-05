package io.github.ArtemV2007.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Spring автоматически внедрит JavaMailSender из стартера spring-boot-starter-mail
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Метод для отправки уведомления на почту.
     * @param email  Почта получателя
     * @param action Тип операции ("CREATE" или "DELETE")
     */
    public void sendNotification(String email, String action) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        // Указываем адрес отправителя (в учебных целях можно любой)
        message.setFrom("noreply@yourdomain.com");

        if ("CREATE".equalsIgnoreCase(action)) {
            message.setSubject("Добро пожаловать!");
            message.setText("Здравствуйте! Ваш acкayнт нa caйтe вaш caйт был yспeшнo coздaн.");
        } else if ("DELETE".equalsIgnoreCase(action)) {
            message.setSubject("Аккаунт удален");
            message.setText("Здравствуйте! Ваш аккаунт был удалён.");
        } else {
            // Если пришел неизвестный экшен, логируем или просто игнорируем
            System.out.println("Неизвестное действие для отправки email: " + action);
            return;
        }

        // Отправка сообщения
        mailSender.send(message);
    }
}
