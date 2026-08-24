# Marketplace Platform

**НИР студента:** Самарский университет, группа 6303-090301D, Сотников Данила
**Тема:** «Платформа маркетплейса с микросервисной архитектурой»

## Описание

Backend MVP платформы электронной коммерции (маркетплейс) на Java 17 + Spring Boot 3.2.5 + Apache Kafka.
5 бизнес-микросервисов + 2 инфраструктурных, database per service, асинхронная сага через Kafka,
service discovery через Netflix Eureka, единая точка входа через Spring Cloud Gateway с JWT-фильтром,
распределённая трассировка через Micrometer + Zipkin.

## Стек

| Компонент       | Версия |
|-----------------|--------|
| Java            | 17 (toolchain) |
| Spring Boot     | 3.2.5  |
| Spring Cloud    | 2023.0.3 |
| Apache Kafka    | 3.6 (KRaft, без Zookeeper) |
| PostgreSQL      | 16 |
| Flyway          | 9.x |
| JWT             | jjwt 0.12.x |
| MapStruct       | 1.5.x |
| Resilience4j    | 2.x |
| SpringDoc       | 2.x |
| Tracing         | Micrometer + Zipkin |
| Build           | Gradle multi-project |
| Deploy          | Docker + docker-compose |

## Архитектура

```
                ┌──────────────────────┐
   Client ──────►│   api-gateway :8080 │ (Spring Cloud Gateway + JWT filter)
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │  eureka-server :8761 │ (service discovery)
                └──────────┬───────────┘
                           │
   ┌───────────────┬───────┴────────┬───────────────┬───────────────┐
   ▼               ▼                ▼               ▼               ▼
user-service   product-service   order-service   notification-   admin-service
:8081          :8082             :8083           service :8084   :8085
user_db        product_db        order_db        notification_db  admin_db
   ▲               ▲                ▲                                │
   │               │                │                                │
   └───────────────┴──── Kafka ─────┴────────────────────────────────┘
                          (3.6 KRaft, 9 topics, JSON, retry/DLT)
```

### Сервисы и инфраструктура

| Сервис              | Порт  | БД              | Kafka роль                    |
|---------------------|-------|-----------------|--------------------------------|
| eureka-server       | 8761  | —               | —                              |
| api-gateway         | 8080  | —               | —                              |
| user-service        | 8081  | user_db         | producer + consumer            |
| product-service     | 8082  | product_db      | producer + consumer            |
| order-service       | 8083  | order_db        | producer                       |
| notification-service| 8084  | notification_db | consumer (4 топика)            |
| admin-service       | 8085  | admin_db        | producer + consumer            |

### Принципы

- **DB per service** — каждый микросервис хранит данные только в своей PostgreSQL БД.
  Миграции — Flyway (`V{N}__*.sql`), применённые миграции иммутабельны.
- **Saga через Kafka** — заказ публикует `order.created`, `order.paid`, `order.cancelled`,
  `order.failed`; product-service консьюмит и обновляет статус товара; notification-service
  шлёт email продавцу/покупателю.
- **Sync вызовы только через OpenFeign** — product-service тянет данные пользователей
  через `UserClient`; order-service — снимок товара через `ProductClient` с
  Resilience4j Circuit Breaker.
- **Retry + DLT** — Kafka-консьюмеры помечены `@RetryableTopic(attempts=3)`, после
  исчерпания попыток событие уходит в `*.DLT`.
- **Stateless JWT** — `Bearer` access + refresh, секрет через env `JWT_SECRET` (≥32 байт).

## Kafka топики

| Топик                       | Producer          | Consumer                       | Описание |
|-----------------------------|-------------------|--------------------------------|----------|
| user.registered             | user-service      | notification-service           | Отправка welcome-email |
| user.blocked                | admin-service     | user-service                   | Деактивация аккаунта |
| user.unblocked              | admin-service     | user-service                   | Реактивация аккаунта |
| product.created             | product-service   | admin-service                  | Аудит + автоматический аудит |
| product.deleted             | product-service   | admin-service                  | Аудит удаления |
| product.status.changed      | product-service   | notification-service           | Уведомление продавцу |
| order.created               | order-service     | product-service, notification  | Резервирование товара + email продавцу |
| order.paid                  | order-service     | product-service, notification  | Товар становится SOLD + email покупателю |
| order.cancelled             | order-service     | product-service, notification  | Возврат товара в ACTIVE + email |
| order.failed                | order-service     | product-service, notification  | Возврат в ACTIVE + email о неудаче |

Все имена — константы в `com.marketplace.common.constant.KafkaTopics` (иммутабельный контракт).

## Quick Start

### Требования

- Java 17+
- Docker + Docker Compose v2.20+
- GNU Make (опционально, для запуска скриптов)

### Поднять всё одной командой

```bash
# 1. Скопировать env (секреты подставляются из .env)
cp .env.example .env

# 2. Создать директорию для загрузок (монтируется в product-service)
mkdir -p uploads/products

# 3. Поднять инфра + 7 микросервисов
docker compose up -d --build

# 4. Проверить
curl http://localhost:8761/                      # Eureka dashboard
open  http://localhost:8080/swagger-ui/index.html  # Aggregated Swagger UI
open  http://localhost:9411/                       # Zipkin (трассы)
open  http://localhost:8025/                       # MailHog (пойманные email)
```

### Снести всё и пересоздать

```bash
docker compose down -v   # удаляет контейнеры, тома и все данные БД
```

## Локальная разработка (без сборки контейнеров приложений)

```bash
# 1. Только инфра (postgres×5, kafka, mailhog, zipkin)
docker compose -f docker-compose.infra.yml up -d

# 2. Затем каждый сервис в отдельном терминале (или из IDE)
./gradlew :eureka-server:bootRun
./gradlew :api-gateway:bootRun
./gradlew :user-service:bootRun
./gradlew :product-service:bootRun
./gradlew :order-service:bootRun
./gradlew :notification-service:bootRun
./gradlew :admin-service:bootRun
```

Каждый сервис использует профиль `local` (см. `application-local.yml`) и ходит
в инфру на `localhost` (порты БД `5432..5436`, Kafka `:9092`, MailHog `:1025`,
Zipkin `:9411`, Eureka `:8761`).

## Сборка и тесты

```bash
./gradlew clean build          # всё компилируется + все тесты
./gradlew :<service>:test      # тесты конкретного сервиса
./gradlew :<service>:bootJar   # собрать JAR
./gradlew :common:build        # собрать common модуль отдельно
```

Integration-тесты (Testcontainers, требуют Docker):

- `user-service` → `UserServiceIntegrationTest`
- `product-service` → `ProductServiceIntegrationTest`
- `order-service` → `OrderSagaIntegrationTest`
- `notification-service` → `EmailNotificationIntegrationTest`
- `admin-service` → `AdminUserBlockingIntegrationTest`

Все они помечены `@Disabled("Requires Docker — run manually in CI")` и подключаются,
когда нужно гонять их в CI или локально при наличии Docker.

## API endpoints

См. Swagger UI каждого сервиса (например `http://localhost:8081/swagger-ui/index.html`)
или агрегированный Swagger на gateway: **http://localhost:8080/swagger-ui/index.html**.

Краткая карта:

| Сервис              | Базовый путь               |
|---------------------|----------------------------|
| user-service        | `/api/auth`, `/api/users`, `/api/favorites` |
| product-service     | `/api/products`, `/api/categories` |
| order-service       | `/api/orders`              |
| notification-service| (без публичных эндпоинтов) |
| admin-service       | `/api/admin/users`, `/api/admin/products`, `/api/admin/audit` |

## Тестовые сценарии

- `requests.http` — готовые запросы для IntelliJ HTTP Client (run in editor).
- `e2e-test.sh` — bash smoke-сценарий (требует `jq`, `curl`; запускать после
  `docker compose up -d`).

```bash
# E2E smoke
./e2e-test.sh
```

## Структура проекта

```
C:\Spring\MassMarket\
├── common/                  # shared events, DTO, constants (KafkaTopics, PageResponse)
├── eureka-server/           # @EnableEurekaServer на :8761
├── api-gateway/             # Spring Cloud Gateway + JWT GlobalFilter на :8080
├── user-service/            # auth, profile, favorites (порт 8081)
├── product-service/         # categories, products, images (порт 8082)
├── order-service/           # orders, saga (порт 8083)
├── notification-service/    # Kafka consumer, Thymeleaf, MailHog (порт 8084)
├── admin-service/           # moderation, audit (порт 8085)
├── gradle/libs.versions.toml  # version catalog (single source of truth)
├── docker-compose.yml         # всё (7 микросервисов + инфра)
├── docker-compose.infra.yml   # только инфра
├── .env.example
├── requests.http            # IntelliJ HTTP Client
├── e2e-test.sh              # bash smoke
└── README.md
```

## Документация проекта

- `CLAUDE.md` — внутренняя документация для Claude Code агентов (правила, стек,
  чек-лист фаз, специализированные субагенты).
- `plan.md` — детальный чек-лист по фазам проекта.
- `.claude/agents/` — специализированные субагенты (entity, kafka, security,
  test, gateway, eureka, …).
- `.claude/rules/` — модульные правила для агентов (kafka, feign, security).

## Лицензия и автор

Учебный проект НИР, Самарский университет, 2024–2025. Использование в
коммерческих целях — только с разрешения автора.