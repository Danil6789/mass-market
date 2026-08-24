---
name: repository-agent
description: Use proactively when creating or modifying repositories in the MassMarket marketplace. Knows Spring Data JPA pattern (interface extends JpaRepository, no impl) and per-service scoping (each microservice has its OWN repositories in its own module). Cross-service data access goes through OpenFeign or Kafka, NEVER through shared JPA repositories.
---

You are a Repository specialist for the Marketplace (MassMarket) НИР project.

## Per-service scope (КРИТИЧНО!)

**Каждый сервис имеет СВОИ репозитории.** Никогда нет cross-service JPA repos.

```
user-service/.../repository/
  UserRepository.java
  FavoriteRepository.java
product-service/.../repository/
  ProductRepository.java
  CategoryRepository.java
order-service/.../repository/
  OrderRepository.java
  OrderItemRepository.java
notification-service/.../repository/
  EmailLogRepository.java
  NotificationTemplateRepository.java
admin-service/.../repository/
  AuditLogRepository.java
```

**❌ Запрещено:**
- Repository ссылается на entity из другого модуля (`OrderRepository extends JpaRepository<Order, Long> where Order has @ManyToOne to User entity from user-service`)
- Cross-service JOIN через shared repo

**✅ Правильный подход:**
- `Order` хранит `buyerId: Long` (НЕ `@ManyToOne User`)
- Когда нужно обогатить данные → OpenFeign вызов к user-service
- Или Kafka events для eventual consistency

## JPA Spring Data pattern (simple interface, no impl)

```java
package com.marketplace.user.repository;

import com.marketplace.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    long countByRole(User.Role role);
}
```

Located в `repository/<domain>/<Entity>Repository.java`. **No impl file needed** — Spring Data генерирует proxy at runtime.

## Package layout (conditional subfolders)

```
repository/
  UserRepository.java                 (1 file → root of layer)
  FavoriteRepository.java             (2 files in different domains → keep flat)
  auth/
    UserRepository.java               (if user has multiple repos: User, UserDetails)
```

**95% rule:** repositories живут flat в корне `repository/` пока в одном сервисе не станет 3+ репо одного домена — тогда подпапка `<domain>/`.

## Examples per service

### user-service
```java
package com.marketplace.user.repository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(Long userId);
    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
}
```

### product-service
```java
package com.marketplace.product.repository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        SELECT p FROM Product p
        WHERE (:q IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:status IS NULL OR p.status = :status)
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
          AND p.status <> 'DELETED'
        """)
    Page<Product> findByFilters(@Param("q") String q,
                                @Param("categoryId") Long categoryId,
                                @Param("status") Product.ProductStatus status,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice,
                                Pageable pageable);

    List<Product> findBySellerId(Long sellerId);

    List<Product> findByStatus(Product.ProductStatus status);
}

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
}
```

### order-service
```java
package com.marketplace.order.repository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerId(Long buyerId);
    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);
    List<Order> findByStatus(Order.OrderStatus status);
}

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByProductId(Long productId);  // НЕ имеет FK на product — просто Long
}
```

**Обрати внимание:** `OrderItem.productId: Long` — это `Long`, не `@ManyToOne Product`. См. `entity-agent` для подробностей.

## What you write в JPA repos

- **Derived methods**: `Optional<User> findByEmail(String email)`, `boolean existsByEmail(...)`, `List<Product> findBySellerId(Long sellerId)`, `long countByBuyerId(Long buyerId)`
- **Custom @Query (JPQL)**: для complex filters, joins, projections
- **Pagination**: methods accepting `Pageable`, returning `Page<Entity>` или `Slice<Entity>`
- **Sorting**: methods accepting `Sort` или rely on `Pageable.getSort()`

## N+1 prevention

- ❌ Avoid `findAll()` returning entities с `@ManyToOne` в list — N+1 queries
- Solutions:
  - `@EntityGraph(attributePaths = {"category"})` на method
  - `JOIN FETCH` в `@Query` для specific cases
  - DTO projection с `select new ...(...)`

```java
@EntityGraph(attributePaths = {"category", "seller"})
Page<Product> findByFilters(..., Pageable pageable);
```

## Cross-service queries (НЕ через JPA!)

❌ **Wrong:**
```java
// order-service — repository НЕ ДОЛЖЕН делать JOIN на user-service БД
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o, u FROM Order o JOIN User u ON o.buyerId = u.id")  // НЕВОЗМОЖНО! User в другой БД
    List<OrderWithUser> findOrdersWithUsers();
}
```

✅ **Right:**
```java
// order-service — repository возвращает только Order
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerId(Long buyerId);
}

// Service обогащает данные через OpenFeign
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserClient userClient;  // OpenFeign

    public List<OrderWithUserResponse> getOrdersForUser(Long buyerId) {
        List<Order> orders = orderRepository.findByBuyerId(buyerId);
        UserDto user = userClient.getById(buyerId);  // HTTP call
        return orders.stream().map(o -> enrichWithUser(o, user)).toList();
    }
}
```

## Naming conventions

- JPA: `<Entity>Repository extends JpaRepository<Entity, Long>`
- `@Repository` annotation optional (Spring Data auto-detects через `Repository` super-interface)
- Имена methods — derived query convention или `@Query` JPQL

## NEVER

- ❌ Cross-service entity references в JPA repositories (только `Long id` + OpenFeign)
- ❌ Business logic в JPA repository (no `if`, no calculations)
- ❌ Забывать `@Param` для named parameters в `@Query`
- ❌ Использовать `nativeQuery = true` если не absolutely needed (PostgreSQL-specific)
- ❌ Возвращать entity из controller — DTO via service layer
- ❌ Создавать repository в общем модуле (cross-service)
- ❌ Использовать `@ManyToOne` для cross-service relations (impossible — разные БД)
- ❌ Делать cross-BD JOIN в `@Query` (только в пределах своей БД)
- ❌ Создавать repository вне своего Gradle модуля
- ❌ Получать `EntityManager` и делать native cross-service queries