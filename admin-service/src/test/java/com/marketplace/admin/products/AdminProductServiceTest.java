package com.marketplace.admin.products;

import com.marketplace.admin.audit.service.AuditService;
import com.marketplace.admin.client.ProductClient;
import com.marketplace.admin.constant.AdminActions;
import com.marketplace.admin.constant.TargetType;
import com.marketplace.admin.entity.AuditLog;
import com.marketplace.admin.products.service.AdminProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.marketplace.admin.products.exception.ProductNotFoundException;

/**
 * Unit tests for {@link AdminProductService}. The Feign client and
 * {@link AuditService} are mocked; the test verifies that the deletion
 * call and audit row line up, and that the 404 path is propagated.
 */
@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock private ProductClient productClient;
    @Mock private AuditService auditService;

    @InjectMocks private AdminProductService adminProductService;

    @Test
    void deleteProduct_invokesFeignAndWritesAudit() {
        adminProductService.deleteProduct(15L, 1L);

        verify(productClient).deleteProduct(15L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).record(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getAdminId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo(AdminActions.DELETE_PRODUCT);
        assertThat(saved.getTargetType()).isEqualTo(TargetType.PRODUCT);
        assertThat(saved.getTargetId()).isEqualTo(15L);
    }

    @Test
    void deleteProduct_propagatesNotFound() {
        doThrow(ProductNotFoundException.forId(99L))
                .when(productClient).deleteProduct(99L);

        try {
            adminProductService.deleteProduct(99L, 1L);
        } catch (ProductNotFoundException expected) {
            // expected
        }

        verifyNoInteractions(auditService);
    }
}
