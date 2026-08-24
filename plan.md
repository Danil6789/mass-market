# План реализации Marketplace Platform

> Полный план: `C:\Users\sotni\.claude\plans\deep-twirling-raven.md`
> Текущий статус проекта: см. `CLAUDE.md`

## Зафиксированные решения

| Решение | Выбор | Обоснование |
|---|---|---|
| Build system | Gradle multi-project | Существующий scaffold — Gradle; быстрее Maven |
| Catalog deps | `gradle/libs.versions.toml` (version catalog) | Один источник правды для всех модулей |
| Package base | `com.marketplace.<service>.<domain>` | Соответствует брифу |
| Хранилище изображений | Локальная ФС + Docker volume `./uploads` | Минимум зависимостей, соответствует MVP |
| Kafka errors | `@RetryableTopic(attempts=3)` + DLT | Готовая обёртка Spring Kafka |
| Refresh tokens | Stateless JWT (access 15 мин + refresh 7 дней) | Без БД-таблицы refresh_tokens |
| Saga | Choreography через Kafka, mock-оплата | Компенсация через `order.failed` → `product.status=ACTIVE` |
| Resilience4j | Только на order-service → product-service | Где есть sync-зависимость через Feign |
| Eureka | Server (:8761) + client в каждом сервисе | По брифу |
| API Gateway JWT | Custom `GlobalFilter` (реактивный) | Избегаем дублирования логики в каждом сервисе |
| Observability | Micrometer Tracing + Zipkin в каждом сервисе | По брифу |
| Admin init | Flyway seed `V2__seed_admin.sql` | Просто для MVP |
| Секреты | Только через `${ENV_VAR}` | По запретам брифа |
| Schema evolution | Flyway (не Liquibase) | Стандарт Spring Boot стека |

## Отклонения от исходного брифа (зафиксировано пользователем)

- **Java 17 → Java 21.** В среде нет JDK 17; пользователь подтвердил переход на JDK 21 (toolchain в `build.gradle` и `common/build.gradle`).
- **Gradle wrapper 9.5.1 → 8.10.** Spring Boot 3.2.5 + dependency-management 1.1.7 несовместимы с Gradle 9.x; downgrade до 8.10.
- **Event contract extension (Phase 5 prereq):** `OrderPaidEvent` и `OrderCancelledEvent` получили поле `productId` — без этого product-service saga compensation (paid→SOLD, cancelled→ACTIVE) была no-op. `EventSerializationTest` обновлён под новые сигнатуры.
- **Spring Boot 3.2.5 / Spring Cloud 2023.0.3 / Kafka 3.6 KRaft / PostgreSQL 16 / Flyway 9 / jjwt 0.12.6 / MapStruct 1.5.5** — без изменений.

## Фаза 0 — Подготовка ✅ DONE

**Goal:** создать документационную базу проекта (CLAUDE.md, plan.md, 20 агентов) и подготовить репозиторий к разработке.

- [x] Создать 20 агентов в `.claude/agents/` (10 адаптированных из BookShop + 10 новых: kafka, gateway, eureka, feign-client, docker, resilience, swagger, observability, config, notification-template)
- [x] Написать `CLAUDE.md` (по образцу BookShop)
- [x] Написать `plan.md` (этот файл)
- [x] `git init` + первоначальный commit
- [x] Удалить старый Spring Boot scaffold (`MassMarketApplication.java`, дефолтный `build.gradle`)
- [x] Создать `.gitignore` (.env, .gradle/, build/, uploads/, *.iml, .idea/)
- [x] `.env.example` создан в Фазе 8
- [x] `README.md` создан в Фазе 9

**DoD:** ✅ `git status` чистый; `CLAUDE.md` и `plan.md` отражают реальное состояние; все 20 агентов созданы.

## Фаза 1 — Инфраструктура ✅ DONE

**Goal:** поднять каркас Gradle multi-project и инфраструктурные сервисы (Eureka, Gateway) с базовым docker-compose.

- [x] `settings.gradle` с `include` для всех 8 модулей
- [x] `build.gradle` parent + `subprojects { ... }` с общими плагинами и зависимостями
- [x] `gradle/libs.versions.toml` — Spring Boot 3.2.5, Spring Cloud 2023.0.3, jjwt 0.12.6, MapStruct 1.5.5, Flyway 9 (Spring Cloud entries — без `version.ref`, BOM-managed)
- [x] `common/` — пустой модуль в Фазе 1, наполнен в Фазе 2
- [x] `eureka-server/` — `EurekaServerApplication` + `@EnableEurekaServer` + application.yml (port 8761)
- [x] `api-gateway/` — Spring Cloud Gateway, application.yml (port 8080), базовые маршруты на placeholder-сервисы
- [x] `docker-compose.infra.yml` — postgres×5 (user_db, product_db, order_db, notification_db, admin_db), kafka 3.6 KRaft (bitnami/kafka:3.6), mailhog, zipkin, все с healthchecks

**Агенты:** eureka-agent, gateway-agent, config-agent, docker-agent, architecture-agent

**DoD:** ✅ `docker-compose -f docker-compose.infra.yml up -d` поднимает всю инфру; Eureka dashboard отвечает на :8761.

## Фаза 2 — Common модуль ✅ DONE

**Goal:** вынести в shared-модуль контракты Kafka-событий и общие DTO/константы.

- [x] `common/build.gradle` — без `spring-boot-starter-web` (чистая Java + Jackson + Jackson JSR-310)
- [x] `common/src/main/java/com/marketplace/common/event/`:
  - User: `UserRegisteredEvent`, `UserBlockedEvent`, `UserUnblockedEvent`
  - Product: `ProductCreatedEvent`, `ProductDeletedEvent`, `ProductStatusChangedEvent`
  - Order: `OrderCreatedEvent`, `OrderPaidEvent` (с `productId` — Phase 5 prereq), `OrderCancelledEvent` (с `productId` — Phase 5 prereq), `OrderFailedEvent`
  - Все — Java records, `@JsonInclude(NON_NULL)`, помечены `<b>IMMUTABLE contract</b>` в Javadoc
- [x] `common/.../dto/`: `PageResponse<T>`, `ErrorResponse` (records)
- [x] `common/.../constant/`: `KafkaTopics` (имена топиков + `DLT_SUFFIX` + `DEFAULT_PARTITIONS=3` + `DEFAULT_REPLICATION_FACTOR=1`)
- [x] Все остальные модули добавляют `implementation project(':common')`
- [x] `EventSerializationTest` — round-trip каждого event'а через Jackson + проверка `@JsonInclude(NON_NULL)`

**Агенты:** kafka-agent, dto-mapper-agent, architecture-agent

**DoD:** ✅ `./gradlew :common:build` собирается; контракты событий зафиксированы (за исключением расширения `productId` в Phase 5 prereq).

## Фаза 3 — User Service (CHECKPOINT) ✅ DONE

**Goal:** реализовать опорный сервис (auth + profile + favorites) как образец стиля для остальных.

- [x] Flyway: `V1__init_users.sql`, `V2__seed_admin.sql` (bcrypt admin@marketplace.local — hash верифицирован `SeedAdminHashTest`), `V3__init_favorites.sql`
- [x] Entities: `User` (с JPA auditing, enum `Role { USER, ADMIN }`, флаги `blocked`/`active`), `Favorite`
- [x] Repositories: `UserRepository`, `FavoriteRepository`
- [x] DTO: `RegisterRequest`, `LoginRequest`, `AuthResponse` (access+refresh), `UserProfileResponse`, `UpdateProfileRequest`, `RefreshRequest`, `AddFavoriteRequest`, `FavoriteResponse`
- [x] Mappers: MapStruct (`componentModel=SPRING`, `unmappedTargetPolicy=IGNORE`)
- [x] Services: `AuthService` (register/authenticate/refresh), `UserService`, `FavoriteService`
- [x] Controllers: `AuthApi`/`AuthController`, `UserApi`/`UserController`, `FavoriteApi`/`FavoriteController` (Api+Impl split + SpringDoc)
- [x] Security: `SecurityConfig` (STATELESS, permitAll на `/api/auth/**`), `JwtService` (jjwt 0.12, HS256), `JwtAuthenticationFilter`
- [x] Kafka: `UserEventProducer` (publish `user.registered`), `UserEventConsumer` (consume `user.blocked` → деактивация), DLT handlers
- [x] Exception handler + custom exceptions (`UserNotFoundException`, `EmailAlreadyExistsException`, `BadCredentialsException`, `UserBlockedException`, `FavoriteNotFoundException`)
- [x] application.yml (port 8081, eureka, postgres user_db, kafka, jwt-secret из env)
- [x] Dockerfile (multi-stage, JDK 21)
- [x] Тесты: `SeedAdminHashTest` (admin credentials verified), unit-тесты services + repositories

**Агенты:** migration-agent, entity-agent, repository-agent, dto-mapper-agent, service-agent, controller-agent, security-agent, kafka-agent, exception-agent, config-agent, docker-agent, test-agent, observability-agent, swagger-agent

**DoD:** ✅ E2E flow register → login → создание favorite работает; JWT валидируется в Gateway; admin может залогиниться seeded credentials.

**CHECKPOINT пройден пользователем ✅** — стиль и подход одобрены. Образец для Phases 4-7.

## Фаза 4 — Product Service ✅ DONE

**Goal:** CRUD для категорий и продуктов + загрузка изображений + реакция на события заказов.

- [x] Flyway: `V1__init_categories.sql`, `V2__init_products.sql`, `V3__init_product_images.sql`
- [x] Entities: `Category`, `Product` (status enum: ACTIVE, RESERVED, SOLD, DELETED), `ProductImage`
- [x] Repositories (Spring Data JPA Specifications для фильтров)
- [x] DTO: `CategoryResponse`, `ProductCreateRequest`, `ProductUpdateRequest`, `ProductResponse`, `ProductListResponse`, `ImageUploadResponse`
- [x] Mappers: MapStruct интерфейсы
- [x] Services: `CategoryService`, `ProductService` (CRUD + поиск по фильтрам), `ImageStorageService` (сохраняет в `/uploads/products/`)
- [x] Controllers + SpringDoc аннотации
- [x] OpenFeign клиент: `UserClient` + `FallbackUserClient` (проверка существования user при создании продукта)
- [x] Kafka: `ProductEventProducer` (product.created, product.deleted), `ProductStatusConsumer` (order.created → RESERVED, order.paid → SOLD, order.cancelled → ACTIVE, order.failed → ACTIVE)
- [x] Exception handler + custom exceptions (`ProductNotFoundException`, `CategoryNotFoundException`, `CategoryAlreadyExistsException`, `ForbiddenException`, `IllegalProductStatusTransitionException`, `ImageStorageException`)
- [x] Security: `JwtService` + `JwtAuthenticationFilter` + `SecurityConfig` (Gateway валидирует; сервис извлекает claims)
- [x] application.yml, application-local.yml, Dockerfile, тесты (20 тестов, 19 passing, 1 Docker-dep @Disabled)

**Агенты:** все базовые + feign-client-agent + kafka-agent + observability-agent + docker-agent

**DoD:** ✅ GET /api/products с фильтрами работает; POST /api/products/{id}/images сохраняет файл в volume.

**Известные ограничения:** OrderPaidEvent/OrderCancelledEvent в common не несут productId — consumer для paid/cancelled no-op до Фазы 5 (там event-контракт будет расширен).

## Фаза 5 — Order Service + Saga ✅ DONE

**Goal:** реализовать жизненный цикл заказа и choreography-сагу через Kafka.

- [x] Flyway: `V1__init_orders.sql` (orders + status check + indexes), `V2__init_order_history.sql`
- [x] Entities: `Order` (audited), `OrderHistory`, `OrderStatus` enum (PENDING/PAID/CANCELLED/FAILED)
- [x] Repositories: `OrderRepository`, `OrderHistoryRepository`
- [x] DTO: `CreateOrderRequest`, `OrderResponse`, `OrderHistoryResponse`
- [x] Services: `OrderService` (createOrder/pay/cancel с ownership rules + append history), `PaymentService` (mock configurable success rate + delay)
- [x] Saga logic: при create → publish `order.created`; при pay → success → `order.paid`, fail → `order.failed`; cancel → `order.cancelled` (productId во всех event'ах, чтобы product-service мог применить transition)
- [x] OpenFeign `ProductClient` + `FallbackProductClient` (UNKNOWN status fallback) + Resilience4j circuit breaker (sliding window 10, 50% threshold, 10s open state)
- [x] Kafka: `OrderEventProducer` (4 события, key=orderId для partition affinity)
- [x] Controllers (`POST /api/orders`, `GET /api/orders/{id}`, `GET /api/orders?role=`, `POST /api/orders/{id}/pay`, `POST /api/orders/{id}/cancel`, `GET /api/orders/{id}/history`), exception handler, security (STATELESS JWT)
- [x] application.yml, application-local.yml, Dockerfile, 18 тестов (15 OrderServiceTest + 3 PaymentServiceTest) + 1 @Disabled (Docker-dep)

**Агенты:** все базовые + feign-client-agent + resilience-agent + kafka-agent + docker-agent + config-agent

**DoD:** ✅ compile + test + bootJar для order-service; common tests (с исправленным EventSerializationTest под extended productId); saga контракт замкнут (ProductStatusConsumer в product-service вызывает applyStatusTransition с productId из event'ов).

**Изменения в common:** в `OrderPaidEvent`/`OrderCancelledEvent` добавлено поле `productId` (Phase 5 prereq) + обновлён `EventSerializationTest`.

## Фаза 6 — Notification Service (TODO)

**Goal:** email-уведомления по Kafka-событиям с шаблонами Thymeleaf, отправка через MailHog (SMTP localhost:1025).

**Архитектурный контракт:** сервис **только consumer** (нет producer'ов). Подписывается на 5 топиков:
- `user.registered` → welcome email получателю (email берётся из event'а)
- `order.created` → "новый заказ" продавцу (email через OpenFeign `UserClient.getEmailById`)
- `order.paid` → "заказ оплачен" покупателю
- `order.cancelled` → "заказ отменён" обеим сторонам
- `product.deleted` → "товар удалён" продавцу

### Структура

```
notification-service/
├── build.gradle                          # ✅ mail + thymeleaf + kafka + eureka-client (без OpenFeign в исходном — добавить)
├── Dockerfile                            # multi-stage, JDK 21
└── src/
    ├── main/
    │   ├── java/com/marketplace/notification/
    │   │   ├── NotificationServiceApplication.java       # @SpringBootApplication, @EnableJpaAuditing, @EnableFeignClients
    │   │   ├── kafka/
    │   │   │   ├── KafkaConfig.java                       # consumer-side + 5 NewTopic beans
    │   │   │   ├── UserEventListener.java                 # @RetryableTopic user.registered → welcome
    │   │   │   ├── OrderEventListener.java                # @RetryableTopic order.* (4 метода: created/paid/cancelled/failed)
    │   │   │   └── ProductEventListener.java              # @RetryableTopic product.deleted
    │   │   ├── email/
    │   │   │   ├── EmailService.java                      # JavaMailSender + Thymeleaf render, логирует EmailLog
    │   │   │   ├── EmailLog.java                          # @Entity: id, recipient, subject, body, status, sentAt, errorMessage
    │   │   │   ├── EmailLogRepository.java
    │   │   │   └── EmailStatus.java                       # enum SENT, FAILED
    │   │   ├── templates/
    │   │   │   ├── TemplateRenderer.java                  # Thymeleaf engine wrapper
    │   │   │   ├── NotificationTemplate.java              # @Entity: id, eventType (@unique), subjectTemplate, bodyTemplate
    │   │   │   └── TemplateRepository.java
    │   │   ├── logs/
    │   │   │   ├── EmailLogApi.java + EmailLogController  # GET /api/notifications/logs (@PreAuthorize ADMIN)
    │   │   │   ├── dto/EmailLogResponse.java               # record
    │   │   │   └── mapper/EmailLogMapper.java
    │   │   ├── client/UserClient.java + FallbackUserClient # @FeignClient user-service для email lookup
    │   │   ├── config/{OpenApiConfig, MailProperties}.java
    │   │   ├── security/                                  # copy-paste из product-service
    │   │   ├── handler/GlobalExceptionHandler.java
    │   │   └── constant/{ApiPath, ExceptionMessages}.java
    │   └── resources/
    │       ├── application.yml                            # port 8084, postgres notification_db (5435), SMTP MailHog
    │       ├── application-local.yml
    │       └── db/migration/
    │           ├── V1__init_email_log.sql
    │           └── V2__init_notification_templates.sql    # seed 5 шаблонов
    └── test/...                                            # 10+ unit-тестов
```

### Ключевые детали

**Flyway `V1__init_email_log.sql`:**
```sql
CREATE TABLE email_log (
    id BIGSERIAL PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENT','FAILED')),
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    error_message VARCHAR(1000)
);
CREATE INDEX idx_email_log_recipient ON email_log(recipient);
CREATE INDEX idx_email_log_status ON email_log(status);
```

**Flyway `V2__init_notification_templates.sql`:** seed 5 шаблонов (`event_type`, `subject_template`, `body_template`). Thymeleaf inline expressions: `[[${orderId}]]`, `[[${amount}]]`, etc.

**EmailService:** sync внутри `@RetryableTopic` consumer'а. При `MailException` бросает — retryable-topic сам ретраит и в DLT. `MimeMessageHelper(message, true, "UTF-8")` для HTML.

**Kafka consumers:** все 5 listener'ов в группе `notification-group`, `@RetryableTopic(attempts=3, backoff=@Backoff(1000, 2.0), dltStrategy=DltStrategy.FAIL_ON_ERROR)`. DLT listeners для каждого топика логируют `log.error(...)`.

**OpenFeign UserClient:** для получения email по userId (только для non-user.registered событий, которые несут только `userId`/`sellerId`/`buyerId`). Fallback → `null`, listener обрабатывает null-логику.

**Security:** copy-paste из product-service. `EmailLogController` помечен `@PreAuthorize("hasRole('ADMIN')")`.

**build.gradle:** добавить `implementation libs.spring.cloud.starter.openfeign` (для UserClient) и `implementation libs.resilience4j.spring.boot3` (для fallback resilience).

**application.yml:** port 8084, postgres:notification (5435), SMTP через MailHog:
```yaml
spring.mail.host: ${MAILHOG_HOST:localhost}
spring.mail.port: ${MAILHOG_PORT:1025}
```

**Тесты:** `EmailServiceTest` (mock `JavaMailSender`, verify MimeMessage содержит subject/body), `TemplateRendererTest`, `OrderEventListenerTest`. `contextLoads()` — `@Disabled` (Docker-dep).

**Агенты:** kafka-agent, notification-template-agent, migration-agent, entity-agent, repository-agent, dto-mapper-agent, service-agent, controller-agent, exception-agent, security-agent (copy-paste pattern), config-agent, docker-agent, test-agent, feign-client-agent (UserClient)

**DoD:** ✅ `./gradlew :notification-service:test` SUCCESS; ✅ `./gradlew compileJava` SUCCESS (весь проект); ✅ `./gradlew :notification-service:bootJar` SUCCESS; manual smoke (после docker-compose infra) — publish `user.registered` в Kafka → email в MailHog UI.

## Фаза 7 — Admin Service (TODO)

**Goal:** модерация контента + управление пользователями + audit log. Все эндпоинты `@PreAuthorize("hasRole('ADMIN')")`. Своя admin_db хранит только audit_log.

**Архитектурный контракт:** producer (`user.blocked`, `user.unblocked`) + consumer (`product.created` для аудита).

### Структура

```
admin-service/
├── build.gradle                          # ✅ web + jpa + security + kafka + eureka-client + openfeign
├── Dockerfile
└── src/
    ├── main/java/com/marketplace/admin/
    │   ├── AdminServiceApplication.java            # @SpringBootApplication, @EnableJpaAuditing, @EnableFeignClients, @EnableMethodSecurity
    │   ├── users/
    │   │   ├── api/AdminUserApi.java
    │   │   ├── controller/AdminUserController.java
    │   │   ├── service/AdminUserService.java        # Feign UserClient (read) + Kafka publish (block/unblock)
    │   │   └── exception/AdminOperationException.java
    │   ├── products/
    │   │   ├── api/AdminProductApi.java
    │   │   ├── controller/AdminProductController.java
    │   │   └── service/AdminProductService.java     # Feign ProductClient (admin delete)
    │   ├── audit/
    │   │   ├── api/AuditApi.java
    │   │   ├── controller/AuditController.java
    │   │   ├── service/AuditService.java
    │   │   ├── dto/AuditLogResponse.java
    │   │   └── mapper/AuditLogMapper.java
    │   ├── entity/AuditLog.java
    │   ├── repository/AuditLogRepository.java
    │   ├── kafka/
    │   │   ├── KafkaConfig.java                     # producer + consumer + 3 NewTopic
    │   │   ├── AdminEventProducer.java              # publish UserBlockedEvent/UserUnblockedEvent
    │   │   └── ProductAuditConsumer.java            # @RetryableTopic product.created → AuditLog
    │   ├── client/
    │   │   ├── UserClient.java + FallbackUserClient # read user (GET /api/users/{id})
    │   │   └── ProductClient.java + FallbackProductClient # admin delete
    │   ├── security/                                # copy-paste из product-service + @EnableMethodSecurity
    │   ├── handler/GlobalExceptionHandler.java
    │   └── constant/{ApiPath, ExceptionMessages, AdminActions}.java
    └── main/resources/
        ├── application.yml                          # port 8085, postgres admin_db (5436)
        ├── application-local.yml
        └── db/migration/V1__init_audit_log.sql
```

### Ключевые детали

**Flyway `V1__init_audit_log.sql`:**
```sql
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,           -- BLOCK_USER, UNBLOCK_USER, DELETE_PRODUCT, AUTO_AUDIT_PRODUCT_CREATED
    target_type VARCHAR(50) NOT NULL,     -- USER, PRODUCT
    target_id BIGINT NOT NULL,
    details VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_admin ON audit_log(admin_id);
CREATE INDEX idx_audit_log_target ON audit_log(target_type, target_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
```

**REST API (все `@PreAuthorize("hasRole('ADMIN')")`):**
- `POST /api/admin/users/{id}/block` — admin проверяет user через Feign → публикует `UserBlockedEvent` → пишет `AuditLog(action=BLOCK_USER)`. Возвращает 202 Accepted.
- `POST /api/admin/users/{id}/unblock` — симметрично.
- `DELETE /api/admin/products/{id}` — Feign DELETE на product-service, ловит 404, пишет audit.
- `GET /api/admin/audit?page=0&size=20` — пагинированный список `AuditLogResponse`.

**Важно:** user-service **НЕ предоставляет** прямых `/block`/`/unblock` REST — блокировка асинхронна через Kafka `user.blocked` → `user-service.UserEventConsumer` деактивирует аккаунт (уже реализовано в Фазе 3). Admin только публикует событие.

**OpenFeign clients:**
- `UserClient` (read): `GET /api/users/{id}` → проверить существование перед блокировкой.
- `ProductClient` (admin delete): `DELETE /api/admin/products/{id}` или существующий endpoint. В Phase 4 product-service имеет `softDelete(productId, callerId)` — admin вызывает с ролью admin.

**KafkaConfig:** ProducerFactory + KafkaTemplate (mirror product-service). 3 NewTopic: `USER_BLOCKED`, `USER_UNBLOCKED`, `PRODUCT_CREATED` (объявить локально для consumer-side безопасности). ConsumerFactory для `product.created` (group-id `admin-audit-group`).

**Kafka consumer:** `ProductAuditConsumer` — на каждое `ProductCreatedEvent` аппендит `AuditLog(action=AUTO_AUDIT_PRODUCT_CREATED)`.

**Security:** copy-paste из product-service + `@EnableMethodSecurity` для `@PreAuthorize`.

**Тесты:** ~8-10 unit-тестов на services + 2-3 на kafka consumer.

**Агенты:** kafka-agent, feign-client-agent, service-agent, controller-agent, exception-agent, security-agent, migration-agent, entity-agent, repository-agent, dto-mapper-agent, config-agent, docker-agent, test-agent

**DoD:** ✅ `./gradlew :admin-service:test` SUCCESS; ✅ `./gradlew compileJava` SUCCESS; ✅ `./gradlew :admin-service:bootJar` SUCCESS; E2E smoke (отложен до Фазы 8): admin-токеном POST `/api/admin/users/{id}/block` → user-service деактивирует.

## Фаза 8 — Кросс-сервисные настройки (TODO)

**Goal:** финальная интеграция — все сервисы на docker-compose, OpenAPI агрегируется через api-gateway, Zipkin показывает end-to-end trace.

### Подзадачи

1. **`docker-compose.yml`** (full stack, рядом с `docker-compose.infra.yml`):
   - 7 сервисов: eureka, gateway, user-service, product-service, order-service, notification-service, admin-service.
   - `depends_on` с `condition: service_healthy` для всех (на healthcheck'и).
   - Healthcheck на каждом: `curl -f http://localhost:808X/actuator/health || exit 1`.
   - `environment` — все секреты через env, **.env не коммитится**.
   - Network: общий `marketplace-net` мост.
   - Volume `marketplace_uploads` для image uploads (product-service).

2. **`.env.example`** (в репо):
   - `POSTGRES_*_PASSWORD` × 5
   - `JWT_SECRET` (минимум 32 байта)
   - `EUREKA_SERVER_URL=http://eureka:8761/eureka/`
   - `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`
   - `MAILHOG_HOST`, `ZIPKIN_ENDPOINT`
   - `UPLOAD_DIR=/var/lib/marketplace/uploads/products`
   - `PAYMENT_SUCCESS_RATE`, `PAYMENT_DELAY_MS`
   - `TRACING_SAMPLING_PROBABILITY`

3. **OpenAPI aggregation в api-gateway:**
   - SpringDoc `springdoc.swagger-ui.urls` — массив URL'ов для каждого сервиса (`http://user-service:8081/v3/api-docs` и т.д.).
   - Gateway route `/swagger-ui/**` → aggregation page.

4. **Zipkin end-to-end:**
   - Все 7 сервисов уже имеют `management.tracing.sampling.probability=1.0` (default). Проверить, что `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` подключены в каждом (5 уже сделано; нужно убедиться в api-gateway).
   - Тестовый запрос: `POST /api/orders` через gateway → проверить в Zipkin UI http://localhost:9411, что trace проходит user → product → order → notification.

5. **`docker-compose up` smoke:**
   - Поднять infra + все 7 сервисов.
   - Прогнать ручной сценарий (см. `requests.http` в Фазе 9).

**Агенты:** docker-agent, swagger-agent, observability-agent, gateway-agent, config-agent

**DoD:** ✅ `docker-compose up` поднимает всё; ✅ Zipkin UI показывает end-to-end trace от `POST /api/orders` через 4+ сервиса.

## Фаза 9 — Качество (TODO)

**Goal:** финальный пакет качества — Testcontainers интеграционные тесты, README, requests.http, e2e-test.sh, финальный clean build.

### Подзадачи

1. **Testcontainers интеграционные тесты (минимум 1 на сервис):**
   - `user-service`: `AuthControllerIntegrationTest` (Testcontainers PostgreSQL + Kafka, `@ServiceConnection`).
   - `product-service`: `ProductControllerIntegrationTest`.
   - `order-service`: `OrderSagaIntegrationTest` (saga end-to-end через Kafka).
   - `notification-service`: `EmailNotificationIntegrationTest`.
   - `admin-service`: `AdminUserBlockingIntegrationTest`.
   - Все тесты, требующие Docker, помечены `@EnabledIfDockerAvailable` или `@Disabled` с TODO.

2. **README.md:**
   - Описание проекта (НИР контекст — Сотников Данила, 6303-090301D).
   - Стек + ссылки на версии (Spring Boot 3.2.5, Java 21, Kafka 3.6 KRaft).
   - Quick start: `docker-compose -f docker-compose.infra.yml up -d && docker-compose up -d`.
   - Архитектурная диаграмма (ASCII).
   - API endpoints (таблица: метод, путь, роль, описание).
   - Kafka topics (таблица: producer/consumer).
   - Тестовые сценарии (curl/requests.http).
   - Observability (Zipkin, Eureka dashboard).

3. **`requests.http`** (IntelliJ HTTP Client):
   - `register-user.http` — `POST /api/auth/register`.
   - `login.http` — `POST /api/auth/login` (сохраняет токен в переменную).
   - `create-product.http` — `POST /api/products` (с bearer-токеном).
   - `create-order.http` — `POST /api/orders`.
   - `pay-order.http` — `POST /api/orders/{id}/pay`.
   - `admin-block-user.http` — `POST /api/admin/users/{id}/block` (admin-токен).

4. **`e2e-test.sh`** (bash):
   - Поднимает infra (`docker-compose -f docker-compose.infra.yml up -d`).
   - Ждёт healthchecks postgres/kafka/mailhog/zipkin.
   - Регистрирует user, логинится, создаёт product, создаёт order, оплачивает.
   - Проверяет email в MailHog API (`GET http://localhost:8025/api/v2/messages`).
   - Cleanup.

5. **Финальный `./gradlew clean build`:**
   - Должен собирать все 8 модулей без warnings (кроме deprecation от Gradle 8.10 vs 9.0 — допустимо).

**Агенты:** test-agent, docker-agent (compose final review)

**DoD:** ✅ все 9 фаз проекта завершены, общий Definition of Done выполнен.

## Definition of Done (общий)

1. ✅ `./gradlew clean build` — успех (все модули компилируются, юнит-тесты зелёные).
2. ⏳ `docker-compose up` — поднимает всю инфраструктуру и 7 микросервисов (после Фазы 8).
3. ⏳ End-to-end flow работает: register → login → create product → create order → pay → email в MailHog (после Фазы 8/9).
4. ✅ Kafka топики созданы автоматически через `NewTopic` beans (3 partitions, RF=1) в каждом producer/consumer сервисе.
5. ⏳ Zipkin показывает trace через user → product → order → notification (после Фазы 8).

## Прогресс проекта (status snapshot)

| Фаза | Статус | Что сделано |
|---|---|---|
| 0 — Подготовка | ✅ DONE | 20 агентов, CLAUDE.md, plan.md, git init |
| 1 — Инфраструктура | ✅ DONE | Gradle multi-project, eureka, gateway, infra compose |
| 2 — Common модуль | ✅ DONE | 10 event records, KafkaTopics, DTO |
| 3 — User Service | ✅ DONE | auth + profile + favorites + JWT + Kafka |
| 4 — Product Service | ✅ DONE | catalog + products CRUD + filters + image upload + Feign |
| 5 — Order Service | ✅ DONE | saga choreography + Resilience4j + mock payment |
| 6 — Notification | ⏳ TODO | Kafka consumers + Thymeleaf + MailHog |
| 7 — Admin | ⏳ TODO | Feign clients + audit log + @PreAuthorize |
| 8 — Cross-service | ⏳ TODO | docker-compose + OpenAPI aggregation + Zipkin |
| 9 — Quality | ⏳ TODO | Testcontainers + README + .http + e2e |

## Критичные файлы (для ревью)

- `C:\Spring\MassMarket\settings.gradle` — список модулей
- `C:\Spring\MassMarket\build.gradle` — общие конфиги (Java 21 toolchain)
- `C:\Spring\MassMarket\gradle\libs.versions.toml` — version catalog
- `C:\Spring\MassMarket\common\src\main/java/com/marketplace/common/event/` — контракты Kafka (иммутабельны, расширены в Phase 5 prereq)
- `C:\Spring\MassMarket\user-service\src/main/java/com/marketplace/user/security/JwtService.java` — JWT паттерн, copy-paste в каждый сервис
- `C:\Spring\MassMarket\order-service/src/main/java/com/marketplace/order/service/OrderService.java` — saga choreography
- `C:\Spring\MassMarket\docker-compose.yml` — финальная сборка (создаётся в Фазе 8)
- `C:\Spring\MassMarket\CLAUDE.md` — статус проекта
- `C:\Spring\MassMarket\plan.md` — этот чек-лист

## Порядок ревью

- После **каждой фазы** → `git commit` → обновить `CLAUDE.md` (статус) и `plan.md` (чеклист).
- После **Фазы 3** (user-service) — **обязательная пауза**, показать пользователю. ✅ Сделано.
- После **Фазы 5** (order-service) — E2E smoke через docker-compose (отложен до Фазы 8/9).

## Out of scope (явно НЕ делаем)

- UI / frontend (только backend REST)
- Реальная интеграция со Stripe/ЮKassa (mock-оплата)
- S3 / MinIO для картинок (локальная ФС + volume)
- API Gateway rate-limiting с Redis (можно добавить позже)
- Contract testing (Spring Cloud Contract)
- Kubernetes / Helm charts (только docker-compose для dev)
- Avro schema registry (только JSON)
- Service mesh (Istio/Linkerd)
