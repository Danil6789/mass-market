package com.marketplace.product.repository;

import com.marketplace.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Product}. Inherits
 * {@link JpaSpecificationExecutor} so dynamic filtering can be done via
 * {@link com.marketplace.product.products.repository.ProductSpecifications}.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    List<Product> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);
}