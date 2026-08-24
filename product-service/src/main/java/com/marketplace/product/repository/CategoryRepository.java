package com.marketplace.product.repository;

import com.marketplace.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Category}.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAsc();

    boolean existsByParentIdAndName(Long parentId, String name);
}