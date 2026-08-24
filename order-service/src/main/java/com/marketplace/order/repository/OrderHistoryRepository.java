package com.marketplace.order.repository;

import com.marketplace.order.entity.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link OrderHistory}.
 */
@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

    List<OrderHistory> findAllByOrderIdOrderByChangedAtAsc(Long orderId);
}
