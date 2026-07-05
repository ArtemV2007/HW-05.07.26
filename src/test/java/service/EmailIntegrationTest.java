package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.github.ArtemV2007.dto.UserEvent;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // Поднимает полный контекст приложения
@AutoConfigureMockMvc // Настраивает инструмент MockMvc для отправки HTTP-запросов
class EmailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 1. Регистрируем расширение GreenMail (встроенный тестовый SMTP-сервер)
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
            .withPerMethodLifecycle(true); // Сбрасывает почтовый ящик перед каждым тестом

    // 2. Переопределяем параметры подключения к почте динамическими портами GreenMail
    @DynamicPropertySource
    static void configureMailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> greenMail.getSmtp().getBindTo());
        registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
        registry.add("spring.mail.username", () -> "test");
        registry.add("spring.mail.password", () -> "test");
        // Отключаем обязательную валидацию схемы БД для этого теста, если Postgres контейнер не запущен
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Test
    @DisplayName("Прямое API должно отправлять корректное письмо при создании пользователя")
    void sendDirectNotification_ShouldSendWelcomeEmail() throws Exception {
        // Arrange
        String targetEmail = "welcome-user@example.com";
        UserEvent event = new UserEvent("CREATE", targetEmail);

        // Act - шлем POST запрос на прямое уведомление
        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());

        // Assert - проверяем, дошло ли письмо на SMTP сервер
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length, "Должно быть получено ровно 1 письмо");

        MimeMessage message = receivedMessages[0];

        // Проверяем получателя, тему и текст
        assertEquals(targetEmail, message.getAllRecipients()[0].toString());
        assertEquals("Добро пожаловать!", message.getSubject());

        String content = (String) message.getContent();
        assertEquals("Здравствуйте! Ваш acкayнт нa caйтe вaш caйт был yспeшнo coздaн.\r\n", content);
    }

    @Test
    @DisplayName("Прямое API должно отправлять корректное письмо при удалении пользователя")
    void sendDirectNotification_ShouldSendDeleteEmail() throws Exception {
        // Arrange
        String targetEmail = "delete-user@example.com";
        UserEvent event = new UserEvent("DELETE", targetEmail);

        // Act
        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());

        // Assert
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        assertEquals(targetEmail, message.getAllRecipients()[0].toString());
        assertEquals("Аккаунт удален", message.getSubject());

        String content = (String) message.getContent();
        assertEquals("Здравствуйте! Ваш аккаунт был удалён.\r\n", content);
    }
}
