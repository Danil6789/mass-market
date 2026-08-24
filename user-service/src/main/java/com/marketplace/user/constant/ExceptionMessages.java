package com.marketplace.user.constant;

/**
 * Russian error message constants used across the user-service.
 *
 * <p>Throwing exceptions should reference these constants via static import
 * ({@code import static com.marketplace.user.constant.ExceptionMessages.*}).
 * Keeping all messages in one place makes i18n and refactoring easier.</p>
 */
public final class ExceptionMessages {

    private ExceptionMessages() {
        // Utility class — no instances.
    }

    public static final String USER_NOT_FOUND = "Пользователь не найден";
    public static final String EMAIL_ALREADY_EXISTS = "Пользователь с таким email уже существует";
    public static final String BAD_CREDENTIALS = "Неверные учётные данные";
    public static final String USER_BLOCKED = "Учётная запись заблокирована";
    public static final String ACCESS_DENIED = "Доступ запрещён";
    public static final String INVALID_TOKEN = "Невалидный или просроченный токен";
    public static final String VALIDATION_FAILED = "Ошибка валидации";
    public static final String INTERNAL_ERROR = "Внутренняя ошибка сервера";
    public static final String FAVORITE_NOT_FOUND = "Товар не найден в избранном";
    public static final String FAVORITE_ALREADY_EXISTS = "Товар уже в избранном";
    public static final String AUTH_REQUIRED = "Требуется аутентификация";
}