package com.saadaoui.tunidesk.category.mapper;

import com.saadaoui.tunidesk.category.dto.CategoryRequest;
import com.saadaoui.tunidesk.category.dto.CategoryResponse;
import com.saadaoui.tunidesk.entity.TicketCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TicketCategory toEntity(CategoryRequest request);

    CategoryResponse toResponse(TicketCategory category);

}
