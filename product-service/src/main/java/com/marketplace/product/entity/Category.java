package com.marketplace.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Product category. Self-referencing via {@code parentId} (no FK — kept as
 * {@link Long} so we can model trees without DB-level cascade constraints).
 *
 * <p>Unique constraint on {@code (parent_id, name)} keeps sibling names unique.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_categories_parent_name",
                columnNames = {"parent_id", "name"}
        ),
        indexes = {
                @Index(name = "idx_categories_parent_id", columnList = "parent_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Category {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Parent category id; {@code null} for top-level categories.
     * Stored as a plain {@link Long} — there is no FK to keep migrations
     * simple and to support future re-parenting.
     */
    @Column(name = "parent_id")
    private Long parentId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}