package io.github.ArtemV2007.service;

import io.github.ArtemV2007.dto.UserEvent;
import io.github.ArtemV2007.dto.UserRequestDTO;
import io.github.ArtemV2007.dto.UserResponseDTO;
import io.github.ArtemV2007.model.User;
import io.github.ArtemV2007.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Имя топика в Kafka
    private static final String TOPIC = "user-events";

    // Spring автоматически внедрит зависимости через конструктор
    public UserService(UserRepository userRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setAge(dto.age());

        User savedUser = userRepository.save(user);

        // Отправляем событие CREATE в Kafka после успешного сохранения
        kafkaTemplate.send(TOPIC, new UserEvent("CREATE", savedUser.getEmail()));

        return mapToResponseDTO(savedUser);
    }

    @Transactional
    public UserResponseDTO createUser(String name, String email, Integer age) {
        return createUser(new UserRequestDTO(name, email, age));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь с ID " + id + " не найден!"));
        return mapToResponseDTO(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь с ID " + id + " не найден!"));

        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setAge(dto.age());
        User updatedUser = userRepository.save(user);
        return mapToResponseDTO(updatedUser);
    }

    @Transactional
    public void updateUser(Long id, String newName, String newEmail, Integer newAge) {
        try {
            updateUser(id, new UserRequestDTO(newName, newEmail, newAge));
        } catch (ResponseStatusException e) {
            System.out.println(e.getReason());
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        // Сначала находим пользователя, чтобы получить его email для отправки уведомления
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь с ID " + id + " не найден!"));

        userRepository.deleteById(id);

        // Отправляем событие DELETE в Kafka после успешного удаления
        kafkaTemplate.send(TOPIC, new UserEvent("DELETE", user.getEmail()));
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getCreatedAt()
        );
    }
}
