package com.marketplace.admin.audit.controller;

import com.marketplace.admin.audit.api.AuditApi;
import com.marketplace.admin.audit.dto.AuditLogResponse;
import com.marketplace.admin.audit.service.AuditService;
import com.marketplace.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the audit-log endpoint. Restricted to ADMIN.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController implements AuditApi {

    private final AuditService auditService;

    @Override
    public ResponseEntity<PageResponse<AuditLogResponse>> list(Pageable pageable) {
        Page<AuditLogResponse> pageResult = auditService.list(pageable);
        PageResponse<AuditLogResponse> body = PageResponse.of(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements()
        );
        return ResponseEntity.ok(body);
    }
}
