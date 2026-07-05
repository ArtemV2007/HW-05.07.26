package service;

import io.github.ArtemV2007.dto.UserEvent;
import io.github.ArtemV2007.dto.UserRequestDTO;
import io.github.ArtemV2007.dto.UserResponseDTO;
import io.github.ArtemV2007.model.User;
import io.github.ArtemV2007.repository.UserRepository;
import io.github.ArtemV2007.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate; // Мокаем отправку сообщений в Kafka

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Создание пользователя должно корректно сохранять объект через репозиторий, отправлять событие в Kafka и возвращать DTO")
    void createUser_ShouldSaveUserViaRepositoryAndReturnDto() {
        // Arrange
        UserRequestDTO requestDto = new UserRequestDTO("Иван", "ivan@example.com", 25);
        User savedUser = new User("Иван", "ivan@example.com", 25);
        savedUser.setId(1L);
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponseDTO responseDto = userService.createUser(requestDto);

        // Assert
        assertNotNull(responseDto);
        assertEquals(1L, responseDto.id());
        assertEquals("Иван", responseDto.name());

        // Проверяем сохранение в БД
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals("Иван", userCaptor.getValue().getName());

        // Проверяем отправку события в Kafka
        verify(kafkaTemplate, times(1)).send(eq("user-events"), eq(new UserEvent("CREATE", "ivan@example.com")));
    }

    @Test
    @DisplayName("Поиск по ID должен возвращать DTO пользователя, если он существует")
    void getUserById_ShouldReturnDto_WhenUserExists() {
        // Arrange
        Long userId = 1L;
        User user = new User("Анна", "anna@example.com", 30);
        user.setId(userId);
        user.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserResponseDTO responseDto = userService.getUserById(userId);

        // Assert
        assertNotNull(responseDto);
        assertEquals(userId, responseDto.id());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Поиск по ID должен выбрасывать исключение 404, если пользователя нет")
    void getUserById_ShouldThrowNotFound_WhenUserDoesNotExist() {
        // Arrange
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> userService.getUserById(userId));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Получение всех пользователей должно маппиться в список DTO")
    void getAllUsers_ShouldReturnListOfDtos() {
        // Arrange
        User user1 = new User("User1", "user1@mail.com", 20);
        user1.setId(1L);
        User user2 = new User("User2", "user2@mail.com", 22);
        user2.setId(2L);

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // Act
        List<UserResponseDTO> actualUsers = userService.getAllUsers();

        // Assert
        assertEquals(2, actualUsers.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Обновление пользователя должно изменять поля и вызывать репозиторий, не отправляя события в Kafka")
    void updateUser_ShouldModifyAndSave_WhenUserExists() {
        // Arrange
        Long userId = 1L;
        User existingUser = new User("Старый", "old@mail.com", 40);
        existingUser.setId(userId);
        UserRequestDTO updateDto = new UserRequestDTO("Новый", "new@mail.com", 45);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // Act
        UserResponseDTO responseDto = userService.updateUser(userId, updateDto);

        // Assert
        assertNotNull(responseDto);
        assertEquals("Новый", existingUser.getName());
        verify(userRepository, times(1)).save(existingUser);
        verifyNoInteractions(kafkaTemplate); // Обновление не должно триггерить Kafka по заданию
    }

    @Test
    @DisplayName("Обновление пользователя должно бросать 404 ошибку, если юзер не найден")
    void updateUser_ShouldThrowNotFound_WhenUserDoesNotExist() {
        // Arrange
        Long userId = 99L;
        UserRequestDTO updateDto = new UserRequestDTO("Имя", "email@mail.com", 30);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> userService.updateUser(userId, updateDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Удаление пользователя должно вызывать удаление в репозитории и отправлять событие в Kafka")
    void deleteUser_ShouldCallRepositoryDelete_WhenUserExists() {
        // Arrange
        Long userId = 5L;
        User user = new User("Клон", "clone@mail.com", 50);
        user.setId(userId);

        // Наш обновленный сервис теперь сначала ищет пользователя через findById
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository, times(1)).deleteById(userId);
        verify(kafkaTemplate, times(1)).send(eq("user-events"), eq(new UserEvent("DELETE", "clone@mail.com")));
    }

    @Test
    @DisplayName("Удаление пользователя должно бросать 404 ошибку, если его нет")
    void deleteUser_ShouldThrowNotFound_WhenUserDoesNotExist() {
        // Arrange
        Long userId = 5L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> userService.deleteUser(userId));
        verify(userRepository, never()).deleteById(anyLong());
        verifyNoInteractions(kafkaTemplate);
    }
}
