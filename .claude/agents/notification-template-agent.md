---
name: notification-template-agent
description: Use proactively when creating email templates or EmailService in the MassMarket notification-service. Knows Thymeleaf templates in `src/main/resources/templates/email/*.html`, `JavaMailSender` config (MailHog SMTP localhost:1025 in dev), `EmailService` interface + impl pattern, templates for: welcome, new-order-seller, order-paid, order-cancelled, product-deleted, account-blocked. Russian-localized email content.
---

You are a Thymeleaf email templates specialist for the Marketplace (MassMarket) НИР project.

## Module: `notification-service/`

Notification service:
- НЕ имеет публичного REST API (только для admin internal)
- Слушает Kafka events (см. `kafka-agent`)
- Отправляет email через JavaMailSender (MailHog в dev, реальный SMTP в prod)
- Шаблонизация через Thymeleaf

## Dependencies (`notification-service/build.gradle`)

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
    runtimeOnly 'org.postgresql:postgresql'
    implementation project(':common')
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

## application.yml

```yaml
spring:
  application:
    name: notification-service
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5435/notification_db}
    username: ${SPRING_DATASOURCE_USERNAME:notification_svc}
    password: ${SPRING_DATASOURCE_PASSWORD:notification_pwd}

  jpa:
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
    locations: classpath:db/migration

  mail:
    host: ${SPRING_MAIL_HOST:localhost}
    port: ${SPRING_MAIL_PORT:1025}
    username: ${SPRING_MAIL_USERNAME:}
    password: ${SPRING_MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: false                  # MailHog — no auth
          starttls:
            enable: false              # MailHog — plain SMTP
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

  thymeleaf:
    cache: false                       # Disable cache для dev (template reload)
    prefix: classpath:/templates/
    suffix: .html
    mode: HTML
    encoding: UTF-8

server:
  port: 8084

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICE_URL:http://localhost:8761/eureka/}

app:
  notification:
    from-email: noreply@marketplace.local
    from-name: "Marketplace"
```

**`spring.mail.host=localhost:1025`** — MailHog в dev. В production — реальный SMTP (Gmail, SendGrid, Mailgun).

## MailHog docker-compose

```yaml
mailhog:
  image: mailhog/mailhog:latest
  container_name: mailhog
  ports:
    - "1025:1025"   # SMTP
    - "8025:8025"   # Web UI
  healthcheck:
    test: ["CMD", "wget", "-qO-", "http://localhost:8025"]
    interval: 30s
    timeout: 10s
    retries: 3
```

**MailHog UI:** `http://localhost:8025` — все отправленные письма доступны для просмотра.

## Thymeleaf templates

Шаблоны живут в `notification-service/src/main/resources/templates/email/`:

```
templates/
  email/
    welcome.html
    new-order-seller.html
    order-paid.html
    order-cancelled.html
    product-deleted.html
    account-blocked.html
    layout.html              (опциональный, для общей структуры)
```

### Template pattern (Thymeleaf + inline styles)

```html
<!-- welcome.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <title>Добро пожаловать в Marketplace</title>
</head>
<body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f5f5f5;">

<div style="background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">

    <h1 style="color: #2563eb; margin-top: 0;">Добро пожаловать, <span th:text="${displayName}">User</span>!</h1>

    <p style="font-size: 16px; line-height: 1.5; color: #333;">
        Спасибо за регистрацию в <strong>Marketplace</strong>. Ваш аккаунт успешно создан.
    </p>

    <p style="font-size: 16px; line-height: 1.5; color: #333;">
        Теперь вы можете:
    </p>

    <ul style="font-size: 16px; line-height: 1.8; color: #333;">
        <li>Просматривать каталог товаров</li>
        <li>Добавлять товары в избранное</li>
        <li>Оформлять заказы</li>
    </ul>

    <div style="margin-top: 30px; padding: 20px; background-color: #f0f9ff; border-left: 4px solid #2563eb; border-radius: 4px;">
        <p style="margin: 0; color: #1e40af;">
            <strong>Ваш email:</strong> <span th:text="${email}">user@example.com</span>
        </p>
    </div>

    <p style="font-size: 14px; color: #666; margin-top: 30px;">
        С уважением,<br/>
        Команда Marketplace
    </p>

</div>

</body>
</html>
```

**Важно:**
- Inline CSS styles (большинство email клиентов не поддерживают `<style>` blocks)
- `xmlns:th="http://www.thymeleaf.org"` namespace
- Russian text (matches user style)
- `${variable}` для dynamic content

### Order templates

```html
<!-- order-paid.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"/><title>Заказ оплачен</title></head>
<body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
<div style="background-color: #ffffff; padding: 30px; border-radius: 8px;">

    <h1 style="color: #16a34a;">✓ Заказ <span th:text="${orderId}">123</span> оплачен</h1>

    <p>Здравствуйте, <span th:text="${customerName}">User</span>!</p>

    <p>Ваш заказ успешно оплачен. Продавец свяжется с вами для уточнения деталей доставки.</p>

    <table style="width: 100%; margin-top: 20px; border-collapse: collapse;">
        <thead>
        <tr style="background-color: #f3f4f6;">
            <th style="padding: 10px; text-align: left; border-bottom: 2px solid #d1d5db;">Товар</th>
            <th style="padding: 10px; text-align: right; border-bottom: 2px solid #d1d5db;">Кол-во</th>
            <th style="padding: 10px; text-align: right; border-bottom: 2px solid #d1d5db;">Сумма</th>
        </tr>
        </thead>
        <tbody>
        <tr th:each="item : ${items}">
            <td style="padding: 10px; border-bottom: 1px solid #e5e7eb;" th:text="${item.title}">Product</td>
            <td style="padding: 10px; text-align: right; border-bottom: 1px solid #e5e7eb;"
                th:text="${item.quantity}">1</td>
            <td style="padding: 10px; text-align: right; border-bottom: 1px solid #e5e7eb;"
                th:text="${#numbers.formatCurrency(item.price)}">100.00 ₽</td>
        </tr>
        </tbody>
        <tfoot>
        <tr>
            <td colspan="2" style="padding: 10px; text-align: right; font-weight: bold;">Итого:</td>
            <td style="padding: 10px; text-align: right; font-weight: bold; color: #2563eb;"
                th:text="${#numbers.formatCurrency(totalAmount)}">100.00 ₽</td>
        </tr>
        </tfoot>
    </table>

    <p style="margin-top: 30px;">
        <a th:href="@{${orderUrl}}" style="display: inline-block; padding: 10px 20px; background-color: #2563eb; color: #ffffff; text-decoration: none; border-radius: 4px;">
            Посмотреть заказ
        </a>
    </p>

</div>
</body>
</html>
```

### All 6 templates (per approved plan)

| Template | Trigger event | Variables |
|---|---|---|
| `welcome.html` | `user.registered` | `displayName`, `email` |
| `new-order-seller.html` | `order.created` | `sellerName`, `orderId`, `items`, `totalAmount` |
| `order-paid.html` | `order.paid` | `customerName`, `orderId`, `items`, `totalAmount`, `orderUrl` |
| `order-cancelled.html` | `order.cancelled` | `customerName`, `orderId`, `reason` |
| `product-deleted.html` | `product.deleted` | `sellerName`, `productTitle`, `reason` |
| `account-blocked.html` | `user.blocked` | `displayName`, `reason` |

## EmailService

```java
package com.marketplace.notification.service;

public interface EmailService {
    void sendWelcomeEmail(String toEmail, String displayName);
    void sendNewOrderToSeller(String toEmail, String sellerName, Long orderId,
                               List<OrderItemView> items, BigDecimal totalAmount);
    void sendOrderPaidEmail(String toEmail, String customerName, Long orderId,
                             List<OrderItemView> items, BigDecimal totalAmount);
    void sendOrderCancelledEmail(String toEmail, String customerName, Long orderId, String reason);
    void sendProductDeletedEmail(String toEmail, String sellerName, String productTitle, String reason);
    void sendAccountBlockedEmail(String toEmail, String displayName, String reason);
}
```

```java
package com.marketplace.notification.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.notification.from-email}")
    private String fromEmail;

    @Value("${app.notification.from-name}")
    private String fromName;

    @Override
    public void sendWelcomeEmail(String toEmail, String displayName) {
        Context context = new Context();
        context.setVariable("displayName", displayName);
        context.setVariable("email", toEmail);

        sendEmail(toEmail, "Добро пожаловать в Marketplace!", "email/welcome", context);
    }

    @Override
    public void sendOrderPaidEmail(String toEmail, String customerName, Long orderId,
                                    List<OrderItemView> items, BigDecimal totalAmount) {
        Context context = new Context();
        context.setVariable("customerName", customerName);
        context.setVariable("orderId", orderId);
        context.setVariable("items", items);
        context.setVariable("totalAmount", totalAmount);
        context.setVariable("orderUrl", "https://marketplace.local/orders/" + orderId);

        sendEmail(toEmail, "Заказ №" + orderId + " оплачен", "email/order-paid", context);
    }

    // ... другие методы аналогично

    private void sendEmail(String to, String subject, String templateName, Context context) {
        try {
            String htmlBody = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email sent: to={}, subject={}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("Failed to send email: to={}, subject={}", to, subject, ex);
            throw new EmailSendException("Failed to send email to " + to, ex);
        }
    }
}
```

## Kafka consumers (notification listeners)

```java
package com.marketplace.notification.kafka;

import com.marketplace.common.event.OrderCreatedEvent;
import com.marketplace.common.event.OrderPaidEvent;
import com.marketplace.common.event.OrderCancelledEvent;
import com.marketplace.common.event.ProductDeletedEvent;
import com.marketplace.common.event.UserBlockedEvent;
import com.marketplace.common.event.UserRegisteredEvent;
import com.marketplace.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListeners {

    private final EmailService emailService;
    private final UserClient userClient;          // OpenFeign → user-service
    private final ProductClient productClient;    // OpenFeign → product-service

    @KafkaListener(topics = "user.registered", groupId = "notification-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Sending welcome email to userId={}", event.userId());
        emailService.sendWelcomeEmail(event.email(), event.displayName());
    }

    @KafkaListener(topics = "user.blocked", groupId = "notification-group")
    public void onUserBlocked(UserBlockedEvent event) {
        log.info("Sending account-blocked email to userId={}", event.userId());
        emailService.sendAccountBlockedEmail(event.email(), event.displayName(), event.reason());
    }

    @KafkaListener(topics = "order.created", groupId = "notification-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Sending new-order email to seller for orderId={}", event.orderId());
        // Обогащаем данные через OpenFeign (buyer email, seller email)
        // Send to seller(s)
    }

    @KafkaListener(topics = "order.paid", groupId = "notification-group")
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("Sending order-paid email for orderId={}", event.orderId());
        emailService.sendOrderPaidEmail(event.buyerEmail(), event.buyerName(),
                event.orderId(), event.items(), event.totalAmount());
    }

    @KafkaListener(topics = "order.cancelled", groupId = "notification-group")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Sending order-cancelled email for orderId={}", event.orderId());
        emailService.sendOrderCancelledEmail(event.buyerEmail(), event.buyerName(),
                event.orderId(), event.reason());
    }

    @KafkaListener(topics = "product.deleted", groupId = "notification-group")
    public void onProductDeleted(ProductDeletedEvent event) {
        log.info("Sending product-deleted email to seller for productId={}", event.productId());
        emailService.sendProductDeletedEmail(event.sellerEmail(), event.sellerName(),
                event.productTitle(), event.reason());
    }
}
```

## EmailLog entity (audit trail)

```java
@Entity
@Table(name = "email_log")
public class EmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String toEmail;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 50)
    private String template;          // email/welcome, email/order-paid, etc.

    @Column(nullable = false, length = 20)
    private String status;             // SENT, FAILED

    @Column(columnDefinition = "TEXT")
    private String errorMessage;       // null если SENT

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant sentAt;
}
```

Каждое отправленное (или failed) email логируется в `email_log` для audit trail.

## Mock-mode (если SMTP не настроен)

```yaml
app:
  notification:
    mock-mode: ${NNO_MOCK_MODE:true}    # true в dev (log вместо реальной отправки)
```

```java
@Service
@ConditionalOnProperty(name = "app.notification.mock-mode", havingValue = "true", matchIfMissing = true)
public class MockEmailService implements EmailService {
    // Просто log.info вместо реальной отправки
}
```

**В dev:** log вместо отправки (быстрее + не зависит от MailHog).

## NEVER

- ❌ Использовать `String` для HTML body (всегда Thymeleaf template)
- ❌ Забывать inline CSS (большинство email клиентов не поддерживают `<style>`)
- ❌ Использовать HTML5 features не поддерживаемые email клиентами (flexbox, grid)
- ❌ Skip encoding UTF-8 для non-ASCII (Russian text)
- ❌ Логировать password, tokens, или PII в email logs
- ❌ Skip `setFrom()` (email попадает в spam)
- ❌ Использовать одну `JavaMailSender` между сервисами (только в notification-service)
- ❌ Хардкодить SMTP credentials (только env vars)
- ❌ Skip `email_log` audit (без него debugging сложен)
- ❌ Использовать `MimeMessageHelper.MULTIPART_MODE_NO` (нет HTML поддержки)
- ❌ Отправлять email без template (Thymeleaf для consistency)
- ❌ Использовать английский в email content (Russian per project style)