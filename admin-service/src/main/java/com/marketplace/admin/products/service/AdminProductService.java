package com.marketplace.admin.products.service;

import com.marketplace.admin.audit.service.AuditService;
import com.marketplace.admin.client.ProductClient;
import com.marketplace.admin.constant.AdminActions;
import com.marketplace.admin.constant.TargetType;
import com.marketplace.admin.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soft-deletes a product via the product-service Feign client and writes
 * the audit row. The 404 case (or product-service unavailable) is raised as
 * {@link com.marketplace.admin.products.exception.ProductNotFoundException}
 * by {@link com.marketplace.admin.client.FallbackProductClient}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductClient productClient;
    private final AuditService auditService;

    @Transactional
    public void deleteProduct(Long productId, Long adminId) {
        productClient.deleteProduct(productId);

        AuditLog row = AuditLog.builder()
                .adminId(adminId)
                .action(AdminActions.DELETE_PRODUCT)
                .targetType(TargetType.PRODUCT)
                .targetId(productId)
                .build();
        auditService.record(row);
        log.info("Admin {} deleted product {}", adminId, productId);
    }
}
