# Marketplace Platform — НИР

## Что это за проект

**Marketplace Platform** — НИР (Самарский ун-т, Сотников Данила, 6303-090301D). Микросервисная платформа электронной коммерции (маркетплейс) на Java 17 + Spring Boot 3.2.x + Apache Kafka. 5 бизнес-сервисов (user, product, order, notification, admin) + 2 инфраструктурных (eureka, api-gateway). Полный план — в `plan.md`.

## Critical Constraints

- **Java 17, Spring Boot 3.2.x** — не менять без согласования. Используем Spring Boot 3.2.5.
- **Kafka 3.6 KRaft mode** (без Zookeeper). JSON-сериализация. Ошибки — `@RetryableTopic(attempts=3)` + DLT через `DeadLetterPublishingRecoverer`.
- **DB per service** — user_db, product_db, order_db, notification_db, admin_db. Никаких shared tables между сервисами.
- **Секреты только через env vars** (`${VAR_NAME}` в application.yml). `.env` в `.gitignore`, `.env.example` в репо.
- **Flyway: применённые миграции иммутабельны** — пиши новую `V{N+1}__*.sql`, не редактируй предыдущие.
- **Eureka для service discovery** — НЕ Spring Cloud Config Server.
- **Gradle multi-project** (не Maven). Version catalog в `gradle/libs.versions.toml`.
- **OpenFeign только для sync вызовов, Kafka только для async событий**. Не смешивать.
- **API docs:** Swagger UI в каждом сервисе на `/swagger-ui/index.html`.
- **День 1 ✅ (20 агентов созданы), День 2 — in progress (CLAUDE.md, plan.md)** — см. checklist ниже.

## Стек

| Компонент | Версия |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Spring Cloud | 2023.0.3 |
| Apache Kafka | 3.6 (KRaft) |
| PostgreSQL | 16 |
| Flyway | 9 |
| JWT | jjwt 0.12.6 |
| MapStruct | 1.5.5 |
| Resilience4j | 2.x |
| SpringDoc OpenAPI | 2.x |
| Tracing | Micrometer Tracing + Zipkin |
| Build | Gradle multi-project |
| Deploy | Docker + docker-compose |

## Команды

```bash
./gradlew clean build                       # полная сборка + тесты
./gradlew :user-service:bootRun             # запуск конкретного сервиса
./gradlew :common:build                     # сборка common модуля
./gradlew :eureka-server:bootRun            # Eureka на :8761
./gradlew :api-gateway:bootRun              # Gateway на :8080

docker-compose -f docker-compose.infra.yml up -d   # инфра (postgres×5, kafka, mailhog, zipkin)
docker-compose up -d                            # + все 7 микросервисов
docker-compose down -v                          # полный сброс
```

## Архитектура (Gradle multi-project)

```
C:\Spring\MassMarket\
├── settings.gradle                 # include для всех модулей
├── build.gradle                    # subprojects { ... } общие конфиги
├── gradle/libs.versions.toml       # version catalog (single source of truth)
├── docker-compose.infra.yml        # postgres×5, kafka, mailhog, zipkin
├── docker-compose.yml              # + все 7 микросервисов
├── .env.example
├── .gitignore
├── CLAUDE.md                       # этот файл
├── plan.md                         # детальный чек-лист
├── common/                         # shared events, DTO, constants
├── eureka-server/                  # :8761 (service discovery)
├── api-gateway/                    # :8080 (Spring Cloud Gateway + JWT filter)
├── user-service/                   # :8081 (auth, profile, favorites)
├── product-service/                # :8082 (categories, products, images)
├── order-service/                  # :8083 (orders, saga via Kafka)
├── notification-service/           # :8084 (email, Thymeleaf templates)
└── admin-service/                  # :8085 (admin operations, audit)
```

## Субагенты

Специализированные субагенты в `C:\Spring\MassMarket\.claude\agents\`. Per-project — НЕ глобальные.

| Агент | Когда звать |
|---|---|
| `architecture-agent` | Новый модуль, проверка зависимостей между Gradle-модулями |
| `controller-agent` | REST эндпоинты (Api+Impl split + SpringDoc) |
| `dto-mapper-agent` | DTO records / `@Data` + MapStruct мапперы |
| `entity-agent` | `@Entity` с Flyway-совместимой схемой, BigDecimal precision |
| `exception-agent` | `GlobalExceptionHandler` + domain exceptions |
| `migration-agent` | Flyway `V*__*.sql` для PostgreSQL |
| `repository-agent` | Spring Data JPA repositories |
| `security-agent` | JWT jjwt 0.12, STATELESS, claim extraction |
| `service-agent` | Бизнес-логика, `@Transactional`, `@Slf4j` |
| `test-agent` | JUnit 5 + Testcontainers `@ServiceConnection` |
| `kafka-agent` | `@RetryableTopic` + DLT, event-классы, сериализация |
| `gateway-agent` | Spring Cloud Gateway routes, JWT `GlobalFilter`, CORS |
| `eureka-agent` | `@EnableEurekaServer` + client config |
| `feign-client-agent` | OpenFeign + Resilience4j integration |
| `docker-agent` | multi-stage Dockerfile per service, docker-compose |
| `resilience-agent` | Circuit Breaker на Feign (только order-service) |
| `swagger-agent` | SpringDoc OpenAPI в каждом сервисе + aggregation |
| `observability-agent` | Micrometer Tracing + Zipkin |
| `config-agent` | application.yml + профили + секреты через env |
| `notification-template-agent` | Thymeleaf + JavaMailSender + MailHog |

**Workflow:**
- Меняю БД → `migration-agent` → `entity-agent` → `repository-agent`
- Новый эндпоинт → `dto-mapper-agent` → `service-agent` → `controller-agent`
- Межсервисное взаимодействие → `kafka-agent` (async) или `feign-client-agent` (sync)
- Новая фича → `architecture-agent` для проверки модульности

## Стиль кода

- **Package**: `com.marketplace.<service>.<domain>` (например `com.marketplace.user.security`).
- **REST**: `XxxApi` interface (SpringDoc аннотации здесь) + `XxxController` (impl).
- **DTO**: records для response, `@Data` с Bean Validation для request.
- **MapStruct**: `componentModel = MappingConstants.ComponentModel.SPRING`, `unmappedTargetPolicy = ReportingPolicy.IGNORE`. Плагин `lombok-mapstruct-binding` в `build.gradle`.
- **Global exception handler** в каждом сервисе: `handler/GlobalExceptionHandler.java`.
- **Static imports** для констант из `constant/` пакета.
- **Lombok**: `@RequiredArgsConstructor` для DI (constructor injection). Никаких `@Autowired` полей.
- **Транзакции**: `@Transactional` на service-методах, read-only где возможно.
- **Логирование**: `@Slf4j` на каждом классе с логикой.

## Modular Docs (правила по компонентам)

| Файл | Когда загружается |
|---|---|
| `.claude/rules/kafka.md` | Kafka producers/consumers, events, retry/DLT |
| `.claude/rules/feign.md` | OpenFeign clients, Resilience4j |
| `.claude/rules/security.md` | JWT, SecurityConfig, Gateway filter |

## Что осталось (статус)

- ☑ Фаза 0 — Подготовка
- ☑ Фаза 1 — Инфраструктура (Gradle multi-project, eureka, gateway, docker-compose.infra)
- ☑ Фаза 2 — Common модуль (events, DTO, constants)
- ☑ Фаза 3 — User Service (**CHECKPOINT пройден** ✅)
- ☑ Фаза 4 — Product Service (CRUD + фильтры JpaSpecificationExecutor + image upload + OpenFeign UserClient + Kafka producer/consumer)
- ☑ Фаза 5 — Order Service + saga (Resilience4j circuit breaker на order→product, mock payment, choreography 4 events)
- ☑ Фаза 6 — Notification Service (Kafka consumers 4 топиков, Thymeleaf inline templates, JavaMailSender, MailHog, EmailLog, OpenFeign UserClient)
- ☐ Фаза 7 — Admin Service
- ☐ Фаза 8 — Кросс-сервисные настройки (OpenAPI aggregation, Zipkin full wiring, docker-compose all services)
- ☐ Фаза 9 — Качество (Testcontainers, README, .http, e2e)

## Frontend

Не входит в scope. Только backend REST + Kafka + docker-compose. UI — отдельный трек, по запросу.

## Файлы, которые не трогаем

- `gradle/wrapper/*` — не редактируем (обновляется через `./gradlew wrapper`).
- `gradlew`, `gradlew.bat` — не редактируем.
- `.env` — в `.gitignore`, секреты оттуда не читаем напрямую в коде.
- `**/db/migration/V*__.sql` после применения — иммутабельны, пишем новую `V{N+1}__*.sql`.
- `gradle/libs.versions.toml` — менять только через явное согласование (общий для всех модулей).
