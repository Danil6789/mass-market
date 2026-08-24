package com.marketplace.admin.constant;

/**
 * Russian error message constants used across the admin-service.
 *
 * <p>Throwing exceptions should reference these constants via static import.</p>
 */
public final class ExceptionMessages {

    private ExceptionMessages() {
        // Utility class — no instances.
    }

    public static final String ACCESS_DENIED = "Доступ запрещён";
    public static final String VALIDATION_FAILED = "Ошибка валидации";
    public static final String INTERNAL_ERROR = "Внутренняя ошибка сервера";
    public static final String AUTH_REQUIRED = "Требуется аутентификация";
    public static final String USER_NOT_FOUND = "Пользователь не найден";
    public static final String PRODUCT_NOT_FOUND = "Товар не найден";
}
