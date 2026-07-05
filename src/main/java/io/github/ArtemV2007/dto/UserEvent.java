package io.github.ArtemV2007.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserEvent(
        @NotBlank(message = "Действие (action) не должно быть пустым")
        String action,

        @NotBlank(message = "Email не должен быть пустым")
        @Email(message = "Некорректный формат email")
        String email
) {}
