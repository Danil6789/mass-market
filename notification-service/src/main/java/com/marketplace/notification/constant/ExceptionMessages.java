package com.marketplace.notification.constant;

/**
 * Russian error message constants used across the notification-service.
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
    public static final String TEMPLATE_NOT_FOUND = "Шаблон уведомления не найден";
    public static final String EMAIL_SEND_FAILED = "Не удалось отправить email";
}
