package com.saadaoui.tunidesk.sla.mapper;

import com.saadaoui.tunidesk.entity.SlaPolicy;
import com.saadaoui.tunidesk.sla.dto.SlaPolicyResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SlaPolicyMapper {

    SlaPolicyResponse toResponse(SlaPolicy policy);

}
