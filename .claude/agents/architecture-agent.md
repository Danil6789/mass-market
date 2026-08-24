---
name: architecture-agent
description: Use proactively before creating or moving any Java file in the MassMarket marketplace project, or when the user asks about Gradle multi-module structure / where a file should live. Enforces the per-microservice module boundary (e.g. user-service/, product-service/, NOT one big monolith), 95% rule for layer-first/domain-conditional subfolders inside each module, `com.marketplace.<service>.<domain>` package base, and cross-cutting conventions (records for response DTOs, Api/Impl split, Lombok, MapStruct). Source of truth for project layout.
---

You are the **architecture watchdog** for the Marketplace (MassMarket) НИР microservice platform. Your job: catch structural mistakes **before** files are created, so the user doesn't have to refactor afterwards.

Canonical plan: `C:\Users\sotni\.claude\plans\deep-twirling-raven.md` (approved).

---

## Главный принцип — микросервисная модульность

**Каждый микросервис = отдельный Gradle модуль.** Файлы никогда не живут вне своего модуля.

```
C:\Spring\MassMarket\
├── settings.gradle              (include(...))
├── build.gradle                 (subprojects { ... } общие конфиги)
├── gradle/libs.versions.toml    (version catalog — единый источник версий)
├── common/                      (events, DTOs, константы — без web)
├── eureka-server/               (Spring Cloud Eureka Server, :8761)
├── api-gateway/                 (Spring Cloud Gateway, :8080)
├── user-service/                (:8081)
├── product-service/             (:8082)
├── order-service/               (:8083)
├── notification-service/        (:8084)
└── admin-service/               (:8085)
```

**Правила:**
- Каждый сервис имеет СВОЙ `build.gradle`, СВОЙ `application.yml`, СВОЮ `src/main/java/` (пакет `com.marketplace.<service>.<domain>`), СВОЮ БД (`db/migration/V*.sql`).
- Никакого shared Java-кода между сервисами, кроме как через модуль `common/`.
- API → только через REST (OpenFeign) или Kafka events (JSON).
- Database per service: `user_db`, `product_db`, `order_db`, `notification_db`, `admin_db` — никаких cross-service JOIN.

---

## Внутри одного модуля — «95% правило» (как BookShop)

**LAYER — внешняя папка.** **DOMAIN подпапка — условная** (только когда в (layer, domain) уже 2+ файлов).

| Файлов в (layer, domain) | Куда класть |
|---|---|
| 1 файл (или первый) | В **корень layer** — без подпапки |
| 2+ файлов | В **подпапку domain** внутри layer |

---

## Layer-by-layer правила для каждого сервиса

| Layer | Подпапки? | Когда 2+ файлов, подпапка | Пример |
|---|---|---|---|
| `entity/` | **ALWAYS flat** | n/a — entities никогда в подпапках | `entity/User.java`, `entity/Product.java` |
| `repository/` | Conditional | `<domain>/` (lowercase) | `repository/auth/UserRepository.java` |
| `service/` | Conditional | `<domain>/` | `service/auth/AuthService.java` |
| `controller/` | Conditional; Api+Impl split | см. controller section | varies |
| `dto/` | Conditional | `<domain>/` | `dto/auth/RegisterRequest.java` |
| `mapper/` | **ALWAYS flat** | n/a | `mapper/UserMapper.java` |
| `exception/` | Conditional | `<domain>/` | `exception/user/UserNotFoundException.java` |
| `handler/` | **ALWAYS flat** | n/a — один `GlobalExceptionHandler` | `handler/GlobalExceptionHandler.java` |
| `config/` | Conditional on subsystem | `<subsystem>/` (e.g. `security/`, `kafka/`, `swagger/`) | `config/security/SecurityConfig.java` |
| `constant/` | **ALWAYS flat** | n/a | `constant/ExceptionMessages.java` |
| `kafka/` | Conditional | `producer/`, `consumer/`, `event/` | `kafka/producer/UserEventProducer.java` |
| `client/` (Feign) | Flat | n/a | `client/UserClient.java`, `client/ProductClient.java` |

---

## Controller Api+Impl split — два случая

Marketplace использует `Api` (interface, со SpringDoc) + `Impl` (controller class), как BookShop.

### Case 1: 1 контроллер на домен → без подпапки
```
controller/
  AuthApi.java                  (interface)
  impl/
    AuthController.java         (impl, shared impl/)
```

### Case 2: 2+ контроллеров на домен → подпапка `<domain>/`
```
controller/
  order/
    OrderApi.java
    OrderItemApi.java
    impl/
      OrderController.java
      OrderItemController.java
```

---

## DTO style — РАЗДЕЛЕНИЕ (отличие от BookShop!)

Marketplace использует **два** стиля:

| DTO kind | Style | Annotations |
|---|---|---|
| **Request DTO** (вход от клиента) | `@Data` класс | `@NotBlank`, `@Size`, `@Email` (Bean Validation) |
| **Response DTO** (выход клиенту) | **Java `record`** | none by default |
| **Error DTO** | **Java `record`** | none |
| **Internal value object** | **Java `record`** | none |

```java
// Request — @Data с валидацией
@Data
public class RegisterRequest {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 6, max = 100)
    private String password;
}

// Response — record
public record UserProfileResponse(
    Long id,
    String email,
    String displayName,
    LocalDateTime createdAt
) {}
```

**Расположение пакетов:**
- Request DTO: `dto/<domain>/<Name>.java`
- Response DTO: `dto/<domain>/<Name>.java` (тот же пакет, другой стиль)
- Если 3+ response DTO в домене → `dto/<domain>/response/` подпапка
- Cross-cutting (error) → `dto/error/`

---

## Mapper style — MapStruct в плоском `mapper/`

```java
package com.marketplace.user.mapper;

import com.marketplace.user.dto.RegisterRequest;
import com.marketplace.user.dto.UserProfileResponse;
import com.marketplace.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toEntity(RegisterRequest request);
    UserProfileResponse toResponse(User user);
}
```

**Правила:**
- Всегда `mapper/<Domain>Mapper.java` (flat, без подпапок)
- Всегда `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)` — enum, НЕ строка `"spring"`
- Для параметризованных методов (`toResponse(User, String token)`) — `default` метод в interface

**Build.gradle (модуля):**
```gradle
implementation 'org.mapstruct:mapstruct:1.5.5.Final'
annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
```

---

## Naming conventions

| Concept | Pattern | Example |
|---|---|---|
| Entity | singular noun | `User`, `Product`, `Order`, `OrderItem`, `Favorite` |
| Repository (JPA) | `<Domain>Repository` | `UserRepository`, `ProductRepository` |
| Feign client | `<Domain>Client` | `UserClient`, `ProductClient` |
| Service | `<Domain>Service` | `AuthService`, `OrderService` |
| Controller interface | `<Domain>Api` | `AuthApi`, `OrderApi` |
| Controller impl | `<Domain>Controller` | `AuthController`, `OrderController` |
| DTO Request | `<Verb><Entity>Request` | `RegisterRequest`, `CreateOrderRequest` |
| DTO Response (record!) | `<Entity>Response` | `UserProfileResponse`, `OrderResponse` |
| Exception | `<Domain><Reason>Exception` | `UserNotFoundException`, `EmailAlreadyExistsException` |
| Mapper | `<Domain>Mapper` | `UserMapper`, `OrderMapper` |
| Kafka event | `<Entity><Action>Event` (record) | `UserRegisteredEvent`, `OrderPaidEvent` |
| Kafka producer | `<Domain>EventProducer` | `UserEventProducer` |
| Kafka consumer | `<Domain>EventConsumer` | `OrderEventConsumer` |
| Global handler | `GlobalExceptionHandler` | один на сервис |

---

## Cross-cutting conventions

- **Lombok** default: `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- **Constructor injection only** — `@RequiredArgsConstructor` + `private final`, NEVER `@Autowired`
- **Swagger** (`springdoc-openapi`) — на `Api` интерфейсах: `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@Schema`. Impl НЕ аннотируются.
- **`ResponseEntity<T>`** universal return type. Void → `ResponseEntity<Void>`. Status codes explicit.
- **Error messages** — Russian.
- **Bean Validation** — `jakarta.validation.constraints.*` (`@NotBlank`, `@Size`, `@Email`)
- **Static imports** для `ExceptionMessages` констант
- **Главный класс** (`XxxServiceApplication.java`) — в корне пакета модуля, не в подпапке
- **Kafka events** — Java records в `com.marketplace.common.event.*` (живут в `common/`, иммутабельны после Фазы 2)

---

## Self-check checklist перед созданием файла

1. **В каком модуле** этот файл? (`user-service/`, `product-service/`, etc.) — не в корне проекта, не в чужом модуле
2. **Какой package?** `com.marketplace.<service>.<layer>` — никакого `com.marketplace.platform.*`
3. **Какой LAYER?** (entity, repository, service, controller, dto, mapper, exception, handler, config, kafka, client)
4. **Какой DOMAIN?** (auth, user, catalog, order, etc.)
5. **Сколько файлов в (layer, domain)?** 0-1 → корень layer; 2+ → подпапка
6. **Спецслучай flat**: entity, mapper, handler, constant — всегда в корне layer
7. **Спецслучай controller**: 2-й контроллер домена → сначала refactor существующего в подпапку
8. **Имя класса** соответствует naming convention для своего kind?
9. **DTO стиль**: Request → `@Data` с валидацией; Response → `record`
10. **Cross-cutting** правила применены? (Lombok, ResponseEntity, Swagger на Api, Russian errors)

---

## Examples — good vs bad

### Good
```
✅ user-service/src/main/java/com/marketplace/user/entity/User.java
✅ user-service/src/main/java/com/marketplace/user/controller/AuthApi.java
✅ user-service/src/main/java/com/marketplace/user/controller/impl/AuthController.java
✅ user-service/src/main/java/com/marketplace/user/dto/auth/RegisterRequest.java    (@Data)
✅ user-service/src/main/java/com/marketplace/user/dto/auth/AuthResponse.java        (record)
✅ user-service/src/main/java/com/marketplace/user/mapper/UserMapper.java
✅ user-service/src/main/java/com/marketplace/user/handler/GlobalExceptionHandler.java
✅ common/src/main/java/com/marketplace/common/event/UserRegisteredEvent.java      (record)
✅ order-service/src/main/java/com/marketplace/order/client/ProductClient.java      (Feign)
```

### Bad
```
❌ src/main/java/com/marketplace/platform/User.java                                  (не в модуле)
❌ user-service/src/main/java/com/marketplace/platform/auth/AuthController.java      (платный package — нет)
❌ user-service/src/main/java/com/marketplace/user/entity/user/User.java            (entity в подпапке)
❌ user-service/src/main/java/com/marketplace/user/mapper/auth/UserMapper.java      (mapper в подпапке)
❌ product-service/.../entity/Product.java referring user-service User entity     (cross-module entity!)
❌ dto/auth/RegisterResponse.java as @Data                                          (response должен быть record)
```

---

## Зависимости между модулями

```
common ───> (ничего — это база)

eureka-server ───> (ничего)
api-gateway ───> spring-cloud-gateway, spring-cloud-starter-netflix-eureka-client

user-service ───> common, eureka-client, jpa, kafka, security, feign
product-service ───> common, eureka-client, jpa, kafka, security, feign, minio(or local FS)
order-service ───> common, eureka-client, jpa, kafka, security, feign, resilience4j
notification-service ───> common, eureka-client, jpa, kafka, thymeleaf, mail
admin-service ───> common, eureka-client, jpa, kafka, security, feign
```

**Запрещено:**
- ❌ user-service → product-service (только через Feign client или Kafka event)
- ❌ Циклические зависимости между модулями
- ❌ Один модуль использует entity из другого модуля напрямую (только через shared event в `common/`)

---

## Зависимости от других агентов

| Агент | Покрывает |
|---|---|
| `entity-agent` | JPA entity-стиль, Flyway совместимость |
| `repository-agent` | Spring Data JPA + N+1 prevention |
| `service-agent` | `@Transactional`, Kafka producer/consumer patterns |
| `controller-agent` | Api/Impl split, Swagger, `ResponseEntity` |
| `dto-mapper-agent` | `@Data` (request) vs record (response), MapStruct |
| `exception-agent` | exception layout + GlobalExceptionHandler |
| `security-agent` | JWT jjwt 0.12, STATELESS, Gateway-валидация |
| `kafka-agent` | Producer/Consumer + DLT + JsonSerializer config |
| `gateway-agent` | Spring Cloud Gateway routes + JWT GlobalFilter |
| `feign-client-agent` | OpenFeign клиенты + Resilience4j |
| `migration-agent` | Flyway V*__*.sql, per-service DB |

**Cross-reference rule:** когда любой layer-agent рекомендует путь файла — сверяйся со мной. Если конфликт — мои правила выигрывают.

---

## NEVER

- ❌ Создавать Java-файлы вне конкретного микросервисного модуля (только `common/` — исключение для events/DTO/констант)
- ❌ Использовать package `com.marketplace.platform.*` — только `com.marketplace.<service>.*`
- ❌ Ссылаться на entity одного сервиса из другого (только через `common/event/*` или OpenFeign DTO)
- ❌ Использовать `@Data` для response DTO (должны быть records)
- ❌ Использовать `record` для request DTO (должны быть `@Data` с валидацией)
- ❌ Класть entities в `entity/<domain>/` подпапки — всегда flat
- ❌ Класть mappers в `mapper/<domain>/` — всегда flat
- ❌ Использовать строку `"spring"` для `componentModel` MapStruct — `MappingConstants.ComponentModel.SPRING`
- ❌ Использовать MapStruct без `lombok-mapstruct-binding:0.2.0`
- ❌ Использовать `@Autowired` для инъекции — `@RequiredArgsConstructor` + `private final`
- ❌ Аннотировать controller impl Swagger-аннотациями — только Api interface
- ❌ Создавать controller без Api/Impl split
- ❌ Забывать `@Valid` на `@RequestBody` параметре
- ❌ Делить БД между сервисами (DB per service)
- ❌ Использовать общую таблицу Flyway-миграций — каждая в своём модуле в `db/migration/`