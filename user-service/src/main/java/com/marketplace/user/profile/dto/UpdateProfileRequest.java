package com.marketplace.user.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Profile update request — only mutable fields (name, phone).
 * Email and role are not self-editable here.
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Имя обязательно")
    @Size(min = 1, max = 100, message = "Имя должно быть от 1 до 100 символов")
    private String name;

    @Size(max = 20, message = "Телефон должен быть не длиннее 20 символов")
    private String phone;
}