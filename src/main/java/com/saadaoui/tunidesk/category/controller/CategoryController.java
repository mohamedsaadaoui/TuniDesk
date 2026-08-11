package com.saadaoui.tunidesk.category.controller;

import com.saadaoui.tunidesk.category.dto.CategoryRequest;
import com.saadaoui.tunidesk.category.dto.CategoryResponse;
import com.saadaoui.tunidesk.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public CategoryResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {

        return categoryService.update(id, request);
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable UUID id) {

        return categoryService.getById(id);
    }

    @GetMapping
    public List<CategoryResponse> getAll(
            @RequestParam(required = false) Boolean activeOnly) {

        return categoryService.getAll(activeOnly);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable UUID id) {

        categoryService.delete(id);
    }

}
