package com.marketplace.product.constant;

/**
 * Russian error message constants used across the product-service.
 *
 * <p>Throwing exceptions should reference these constants via static import.
 * Keeping all messages in one place makes i18n and refactoring easier.</p>
 */
public final class ExceptionMessages {

    private ExceptionMessages() {
        // Utility class — no instances.
    }

    public static final String PRODUCT_NOT_FOUND = "Товар не найден";
    public static final String CATEGORY_NOT_FOUND = "Категория не найдена";
    public static final String CATEGORY_ALREADY_EXISTS = "Категория с таким именем уже существует";
    public static final String ACCESS_DENIED = "Доступ запрещён";
    public static final String VALIDATION_FAILED = "Ошибка валидации";
    public static final String INTERNAL_ERROR = "Внутренняя ошибка сервера";
    public static final String AUTH_REQUIRED = "Требуется аутентификация";
    public static final String NOT_OWNER = "Только владелец или администратор может выполнить это действие";
    public static final String IMAGE_STORAGE_FAILED = "Не удалось сохранить изображение";
    public static final String INVALID_IMAGE_FORMAT = "Допустимы только JPEG и PNG";
    public static final String IMAGE_TOO_LARGE = "Размер изображения не должен превышать 5 МБ";
    public static final String INVALID_STATUS_TRANSITION = "Недопустимый переход статуса товара";
}