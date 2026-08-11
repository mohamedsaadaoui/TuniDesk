package com.saadaoui.tunidesk.audit.mapper;

import com.saadaoui.tunidesk.audit.dto.AuditLogResponse;
import com.saadaoui.tunidesk.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog log);

}
