package com.marketplace.notification.templates;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link NotificationTemplate}. The renderer
 * looks up templates by {@code eventType} for each incoming Kafka event.
 */
@Repository
public interface TemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByEventType(String eventType);
}
