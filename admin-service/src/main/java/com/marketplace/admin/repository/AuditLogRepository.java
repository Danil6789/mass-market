package com.marketplace.admin.repository;

import com.marketplace.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link AuditLog}. The audit endpoint uses
 * {@code findAllByOrderByOccurredAtDesc(Pageable)} so the most recent events
 * appear first.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Returns audit rows sorted by {@code occurredAt} descending. Method-name
     * derivation keeps the query trivial so it can be handled by the
     * JPA layer without an extra {@code @Query}.
     */
    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
