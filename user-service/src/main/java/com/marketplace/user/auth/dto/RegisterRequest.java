package com.marketplace.user.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Registration request — email + password + name + optional phone.
 *
 * <p>Mutable DTO with Bean Validation. Required for {@code @Valid @RequestBody}
 * to fail fast on missing/invalid fields.</p>
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов")
    private String password;

    @NotBlank(message = "Имя обязательно")
    @Size(min = 1, max = 100, message = "Имя должно быть от 1 до 100 символов")
    private String name;

    @Size(max = 20, message = "Телефон должен быть не длиннее 20 символов")
    private String phone;
}