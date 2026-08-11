package com.saadaoui.tunidesk.audit.service;

import com.saadaoui.tunidesk.audit.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void record(String action, String entityType, Object entityId, String details);

    void record(String action, String entityType, Object entityId, String details, String performedBy);

    Page<AuditLogResponse> getAll(String search, Pageable pageable);

}
