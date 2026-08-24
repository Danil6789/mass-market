package com.marketplace.product.repository;

import com.marketplace.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ProductImage}.
 */
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findAllByProductIdOrderByPositionAscIdAsc(Long productId);
}