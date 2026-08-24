package com.marketplace.order.repository;

import com.marketplace.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Order}.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findAllByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    Page<Order> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
}
