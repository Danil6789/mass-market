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

## Фаза 0 — Подготовка

**Goal:** создать документационную базу проекта (CLAUDE.md, plan.md, 20 агентов) и подготовить репозиторий к разработке.

- [x] Создать 20 агентов в `.claude/agents/`
- [x] Написать `CLAUDE.md` (по образцу BookShop)
- [x] Написать `plan.md` (этот файл)
- [ ] `git init` + первоначальный commit
- [ ] Удалить старый Spring Boot scaffold (`MassMarketApplication.java`, `build.gradle` → пересоздать в Фазе 1)
- [ ] Создать `.gitignore` (.env, .gradle/, build/, uploads/, *.iml, .idea/)
- [ ] Создать `.env.example` со всеми переменными
- [ ] Создать `README.md`

**Агенты:** (нет активных — это подготовительная фаза)

**DoD:** `git status` чистый; `CLAUDE.md` и `plan.md` отражают реальное состояние; все 20 агентов созданы.

## Фаза 1 — Инфраструктура

**Goal:** поднять каркас Gradle multi-project и инфраструктурные сервисы (Eureka, Gateway) с базовым docker-compose.

- [ ] `settings.gradle` с `include` для всех 8 модулей (common, eureka-server, api-gateway, 5 сервисов)
- [ ] `build.gradle` parent + `subprojects { ... }` с общими плагинами и зависимостями
- [ ] `gradle/libs.versions.toml` — Spring Boot 3.2.5, Spring Cloud 2023.0.3, jjwt 0.12.6, MapStruct 1.5.5, Flyway 9
- [ ] `common/` — пустой модуль (наполнится в Фазе 2)
- [ ] `eureka-server/` — `EurekaServerApplication` + `@EnableEurekaServer` + application.yml (port 8761, `register-with-eureka=false`)
- [ ] `api-gateway/` — Spring Cloud Gateway, application.yml (port 8080), базовые маршруты на placeholder-сервисы
- [ ] `docker-compose.infra.yml` — postgres×5 (user_db, product_db, order_db, notification_db, admin_db), kafka (KRaft, 1 broker), mailhog, zipkin
- [ ] README обновить — секция "Запуск"

**Агенты:** eureka-agent, gateway-agent, config-agent, docker-agent, architecture-agent

**DoD:** `./gradlew :eureka-server:bootRun` стартует, dashboard на http://localhost:8761 отвечает; `docker-compose -f docker-compose.infra.yml up -d` поднимает всю инфру.

## Фаза 2 — Common модуль

**Goal:** вынести в shared-модуль контракты Kafka-событий и общие DTO/константы.

- [ ] `common/build.gradle` — без `spring-boot-starter-web` (чистая Java + Jackson)
- [ ] `common/src/main/java/com/marketplace/common/event/`:
  - `UserRegisteredEvent`, `UserBlockedEvent`, `UserUnblockedEvent`
  - `ProductCreatedEvent`, `ProductDeletedEvent`, `ProductStatusChangedEvent`
  - `OrderCreatedEvent`, `OrderPaidEvent`, `OrderCancelledEvent`, `OrderFailedEvent`
  - Все — Java records с полями из брифа
- [ ] `common/.../dto/`: `PageResponse<T>`, `ErrorResponse`
- [ ] `common/.../constant/`: `KafkaTopics` (имена топиков), `SecurityConstants`
- [ ] `common/.../util/`: `JsonUtil` (если нужен)
- [ ] Все остальные модули добавляют `implementation project(':common')`

**Агенты:** kafka-agent, dto-mapper-agent, architecture-agent

**DoD:** `./gradlew :common:build` собирается; контракты событий иммутабельны после Фазы 2.

## Фаза 3 — User Service (CHECKPOINT)

**Goal:** реализовать опорный сервис (auth + profile + favorites) как образец стиля для остальных.

- [ ] Flyway: `V1__init_users.sql`, `V2__seed_admin.sql` (bcrypt admin@marketplace.local), `V3__init_favorites.sql`
- [ ] Entities: `User`, `Favorite`
- [ ] Repositories: `UserRepository`, `FavoriteRepository`
- [ ] DTO: `RegisterRequest`, `LoginRequest`, `AuthResponse` (access+refresh), `UserProfileResponse`, `UpdateProfileRequest`, `FavoriteResponse`
- [ ] Mappers: MapStruct интерфейсы
- [ ] Services: `AuthService` (register/authenticate/refresh), `UserService`, `FavoriteService`
- [ ] Controllers: `AuthApi`/`AuthController`, `UserApi`/`UserController`, `FavoriteApi`/`FavoriteController`
- [ ] Security: `SecurityConfig` (STATELESS, permitAll на /api/auth/**, остальное authenticated), `JwtService` (jjwt 0.12), `JwtAuthenticationFilter`
- [ ] Kafka: `UserEventProducer` (publish `user.registered`), `UserEventConsumer` (consume `user.blocked` → деактивация)
- [ ] Exception handler + custom exceptions (`UserNotFoundException`, `EmailAlreadyExistsException`)
- [ ] application.yml (port 8081, eureka, postgres user_db, kafka, jwt-secret из env)
- [ ] Dockerfile (multi-stage)
- [ ] Тесты: `AuthServiceTest` (unit, >60% coverage), `UserControllerIntegrationTest` (Testcontainers `@ServiceConnection` PostgreSQL + Kafka)

**Агенты:** migration-agent, entity-agent, repository-agent, dto-mapper-agent, service-agent, controller-agent, security-agent, kafka-agent, exception-agent, config-agent, docker-agent, test-agent, observability-agent, swagger-agent

**DoD:** E2E flow register → login → создание favorite работает; JWT валидируется в Gateway; admin может залогиниться seeded credentials.

**🛑 CHECKPOINT:** показать пользователю результат Фазы 3 перед Фазой 4 для одобрения стиля.

## Фаза 4 — Product Service

**Goal:** CRUD для категорий и продуктов + загрузка изображений + реакция на события заказов.

- [ ] Flyway: `V1__init_categories.sql`, `V2__init_products.sql`, `V3__init_product_images.sql`
- [ ] Entities: `Category`, `Product` (status enum: ACTIVE, RESERVED, SOLD, DELETED), `ProductImage`
- [ ] Repositories (Spring Data JPA Specifications для фильтров)
- [ ] DTO: `CategoryResponse`, `ProductCreateRequest`, `ProductUpdateRequest`, `ProductResponse`, `ProductListResponse`, `ImageUploadResponse`
- [ ] Mappers: MapStruct интерфейсы
- [ ] Services: `CategoryService`, `ProductService` (CRUD + поиск по фильтрам), `ImageStorageService` (сохраняет в `/uploads/products/`)
- [ ] Controllers + SpringDoc аннотации
- [ ] OpenFeign клиент: `UserClient` (проверка существования user при создании продукта)
- [ ] Kafka: `ProductEventProducer` (product.created, product.deleted), `ProductStatusConsumer` (order.created → RESERVED, order.paid → SOLD, order.cancelled → ACTIVE, order.failed → ACTIVE)
- [ ] Exception handler + custom exceptions (`ProductNotFoundException`, `InsufficientStockException`)
- [ ] Security: тот же JWT-фильтр, что в user-service (Gateway валидирует; сервис извлекает claims)
- [ ] application.yml, Dockerfile, тесты

**Агенты:** все базовые + feign-client-agent + kafka-agent + observability-agent

**DoD:** GET /api/products с фильтрами работает; POST /api/products/{id}/images сохраняет файл в volume; product получает order.created и переходит в RESERVED.

## Фаза 5 — Order Service + Saga

**Goal:** реализовать жизненный цикл заказа и choreography-сагу через Kafka.

- [ ] Flyway: `V1__init_orders.sql`, `V2__init_order_history.sql`
- [ ] Entities: `Order`, `OrderHistory`, `OrderStatus` enum
- [ ] Repositories
- [ ] DTO: `OrderCreateRequest`, `OrderResponse`, `OrderHistoryResponse`
- [ ] Services: `OrderService` (createOrder, pay, cancel), `PaymentService` (mock: 95% success, 200ms delay)
- [ ] Saga logic: при create → publish `order.created`; при pay → если success, publish `order.paid`, иначе publish `order.failed`
- [ ] OpenFeign `ProductClient` с Resilience4j circuit breaker (при недоступности product-service → возврат ошибки пользователю)
- [ ] Kafka: `OrderEventProducer` (order.created, order.paid, order.cancelled, order.failed)
- [ ] Controllers, exception handler, security
- [ ] application.yml, Dockerfile, тесты

**Агенты:** все базовые + feign-client-agent + resilience-agent + kafka-agent

**DoD:** E2E через docker-compose infra: создание заказа → product-service получает `order.created` → product.status=RESERVED → pay → SOLD. При сбое оплаты → order.failed → product.status=ACTIVE (компенсация).

## Фаза 6 — Notification Service

**Goal:** email-уведомления по Kafka-событиям с шаблонами Thymeleaf.

- [ ] Flyway: `V1__init_email_log.sql`, `V2__init_notification_templates.sql`
- [ ] Entities: `EmailLog`, `NotificationTemplate`
- [ ] Repository
- [ ] Thymeleaf templates в `src/main/resources/templates/email/`:
  - `welcome.html`, `new-order-seller.html`, `order-paid.html`, `order-cancelled.html`, `product-deleted.html`, `account-blocked.html`
- [ ] `EmailService` (JavaMailSender, MailHog SMTP localhost:1025)
- [ ] Kafka consumers (только): `UserRegisteredListener`, `OrderCreatedListener`, `OrderPaidListener`, `OrderCancelledListener`, `ProductDeletedListener` (все в группе `notification-group`)
- [ ] REST controllers для логов и шаблонов (внутренние, через Gateway)
- [ ] application.yml, Dockerfile, тесты (mock JavaMailSender)

**Агенты:** kafka-agent, notification-template-agent, migration-agent, entity-agent, repository-agent, dto-mapper-agent, service-agent, controller-agent

**DoD:** при ручном publish `user.registered` в Kafka → email появляется в MailHog UI (http://localhost:8025).

## Фаза 7 — Admin Service

**Goal:** админ-операции (block/unblock пользователей, удаление продуктов) с аудитом.

- [ ] Flyway: `V1__init_audit_log.sql`
- [ ] Entity: `AuditLog`
- [ ] Repository
- [ ] OpenFeign клиенты: `UserClient` (block/unblock), `ProductClient` (delete product)
- [ ] Services: `AdminUserService`, `AdminProductService`, `AuditService`
- [ ] Kafka: `AdminEventProducer` (user.blocked, user.unblocked), `ProductAuditConsumer` (audit-group)
- [ ] Controllers с `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Security config (тот же JWT, но проверка роли)
- [ ] application.yml, Dockerfile, тесты

**Агенты:** все базовые + feign-client-agent + kafka-agent

**DoD:** admin-токеном можно заблокировать пользователя → user-service получает `user.blocked` → деактивирует аккаунт.

## Фаза 8 — Кросс-сервисные настройки

**Goal:** унифицировать API documentation, observability, финальная сборка docker-compose.

- [ ] SpringDoc OpenAPI в каждом сервисе (`springdoc-openapi-starter-webmvc-ui`)
- [ ] api-gateway агрегирует `/v3/api-docs` через `springdoc.swagger-ui.urls`
- [ ] Micrometer Tracing + Zipkin в каждом сервисе (`management.tracing.sampling.probability=1.0` для dev)
- [ ] `docker-compose.yml` со всеми сервисами + healthchecks + depends_on
- [ ] `.env.example` полный с описанием каждой переменной
- [ ] README — секции API endpoints, observability

**Агенты:** swagger-agent, observability-agent, docker-agent, gateway-agent

**DoD:** `docker-compose up` поднимает всё; в Zipkin UI (http://localhost:9411) видна цепочка trace от POST /api/orders через все 4 сервиса.

## Фаза 9 — Качество

**Goal:** покрытие тестами, документация, e2e smoke-тесты.

- [ ] Testcontainers интеграционные тесты для каждого сервиса (минимум 1 на сервис)
- [ ] Unit-тесты покрывают service-слой (>60% по jacoco)
- [ ] `./gradlew jacocoTestReport` агрегирует coverage
- [ ] README.md полный с инструкцией запуска, переменными, примерами запросов
- [ ] `requests.http` (IntelliJ HTTP Client) с примерами: register → login → create product → create order → pay
- [ ] `e2e-test.sh` (по образцу BookShop)
- [ ] Финальный `./gradlew clean build` — успешен
- [ ] Обновить CLAUDE.md — все фазы ✅

**Агенты:** test-agent

**DoD:** все 9 фаз проекта завершены, общий Definition of Done выполнен.

## Definition of Done (общий)

1. `./gradlew clean build` — успех (все модули компилируются, тесты зелёные).
2. `docker-compose up` — поднимает всю инфраструктуру и 7 микросервисов.
3. End-to-end flow работает: register → login → create product → create order → pay → email в MailHog.
4. Kafka топики созданы автоматически (через `NewTopic` beans или auto.create.topics.enable), события видны в Kafka UI.
5. Zipkin показывает trace через user → product → order → notification.

## Критичные файлы (для ревью)

- `C:\Spring\MassMarket\settings.gradle` — список модулей
- `C:\Spring\MassMarket\build.gradle` — общие конфиги
- `C:\Spring\MassMarket\gradle\libs.versions.toml` — версии
- `C:\Spring\MassMarket\common\src\main/java/com/marketplace/common/event/` — контракты Kafka (иммутабельны после Фазы 2)
- `C:\Spring\MassMarket\api-gateway\src/main/java/com/marketplace/gateway/filter/JwtAuthenticationFilter.java` — security
- `C:\Spring\MassMarket\user-service\src/main/java/com/marketplace/user/security/JwtService.java` — JWT утилиты
- `C:\Spring\MassMarket\order-service/src/main/java/com/marketplace/order/service/OrderService.java` — saga logic
- `C:\Spring\MassMarket\docker-compose.yml` — финальная сборка
- `C:\Spring\MassMarket\CLAUDE.md` — статус проекта
- `C:\Spring\MassMarket\plan.md` — этот чек-лист

## Порядок ревью

- После **каждой фазы** → superpowers code-review → commit → обновить `CLAUDE.md` (✅ для завершённых пунктов) и `plan.md`.
- После **Фазы 3** (user-service) — **обязательная пауза**, показать пользователю, дождаться одобрения стиля.
- После **Фазы 5** (order-service) — E2E smoke через docker-compose, проверить MailHog + Zipkin.

## Out of scope (явно НЕ делаем)

- UI / frontend (только backend REST)
- Реальная интеграция со Stripe/ЮKassa (mock-оплата)
- S3 / MinIO для картинок (локальная ФС + volume)
- API Gateway rate-limiting с Redis (можно добавить позже)
- Contract testing (Spring Cloud Contract)
- Kubernetes / Helm charts (только docker-compose для dev)
- Avro schema registry (только JSON)
- Service mesh (Istio/Linkerd)
