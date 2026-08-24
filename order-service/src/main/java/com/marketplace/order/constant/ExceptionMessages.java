package com.marketplace.order.constant;

/**
 * Russian error message constants used across the order-service.
 *
 * <p>Throwing exceptions should reference these constants via static import.
 * Keeping all messages in one place makes i18n and refactoring easier.</p>
 */
public final class ExceptionMessages {

    private ExceptionMessages() {
        // Utility class — no instances.
    }

    public static final String ORDER_NOT_FOUND = "Заказ не найден";
    public static final String ORDER_OPERATION_FAILED = "Недопустимая операция для текущего статуса заказа";
    public static final String PAYMENT_FAILED = "Оплата не прошла";
    public static final String PRODUCT_NOT_AVAILABLE = "Товар недоступен для заказа";
    public static final String ACCESS_DENIED = "Доступ запрещён";
    public static final String VALIDATION_FAILED = "Ошибка валидации";
    public static final String INTERNAL_ERROR = "Внутренняя ошибка сервера";
    public static final String AUTH_REQUIRED = "Требуется аутентификация";
}
