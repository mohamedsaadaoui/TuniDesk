package com.saadaoui.tunidesk.category.service;

import com.saadaoui.tunidesk.category.dto.CategoryRequest;
import com.saadaoui.tunidesk.category.dto.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(UUID id, CategoryRequest request);

    CategoryResponse getById(UUID id);

    List<CategoryResponse> getAll(Boolean activeOnly);

    void delete(UUID id);

}
