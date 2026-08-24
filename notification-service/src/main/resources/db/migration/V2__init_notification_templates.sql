CREATE TABLE notification_template (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL UNIQUE,
    subject_template VARCHAR(500) NOT NULL,
    body_template TEXT NOT NULL
);

INSERT INTO notification_template (event_type, subject_template, body_template) VALUES
    ('WELCOME',
     'Добро пожаловать в Marketplace, [[${name}]]!',
     'Здравствуйте, [[${name}]]! Ваш аккаунт ([[${email}]]) успешно создан.'),
    ('NEW_ORDER_SELLER',
     'Новый заказ #[[${orderId}]]',
     'Поступил новый заказ #[[${orderId}]] на сумму [[${amount}]].'),
    ('ORDER_PAID',
     'Заказ #[[${orderId}]] оплачен',
     'Ваш заказ #[[${orderId}]] успешно оплачен. Спасибо за покупку!'),
    ('ORDER_CANCELLED',
     'Заказ #[[${orderId}]] отменён',
     'Заказ #[[${orderId}]] был отменён. Причина: [[${reason}]]'),
    ('PRODUCT_DELETED',
     'Ваш товар удалён',
     'Ваш товар #[[${productId}]] был удалён администратором.');
