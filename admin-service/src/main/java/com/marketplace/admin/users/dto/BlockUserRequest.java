package com.marketplace.admin.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /api/admin/users/{id}/block}. Reason is
 * mandatory and bounded at 1000 characters to fit the audit_log.details
 * column without truncation.
 */
@Data
public class BlockUserRequest {

    @NotBlank
    @Size(max = 1000)
    private String reason;
}
