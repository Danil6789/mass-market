package com.marketplace.notification.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link EmailLog}. Admin endpoint uses
 * {@code findAll(Pageable)}; filtering helpers can be added later.
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
}
