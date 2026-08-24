package com.marketplace.notification.logs;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.notification.email.EmailLog;
import com.marketplace.notification.email.EmailLogRepository;
import com.marketplace.notification.logs.dto.EmailLogResponse;
import com.marketplace.notification.logs.mapper.EmailLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the email-log endpoint. Restricted to ADMIN via
 * {@link PreAuthorize}; mirrors the {@code @PreAuthorize} style already
 * used (implicitly) by product-service's category POST.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmailLogController implements EmailLogApi {

    private final EmailLogRepository emailLogRepository;
    private final EmailLogMapper emailLogMapper;

    @Override
    public ResponseEntity<PageResponse<EmailLogResponse>> list(int page, int size) {
        Page<EmailLog> pageResult = emailLogRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"))
        );
        PageResponse<EmailLogResponse> response = PageResponse.of(
                pageResult.getContent().stream().map(emailLogMapper::toResponse).toList(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements()
        );
        return ResponseEntity.ok(response);
    }
}
