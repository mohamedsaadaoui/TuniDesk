package com.saadaoui.tunidesk.category.service.impl;

import com.saadaoui.tunidesk.audit.AuditConstants;
import com.saadaoui.tunidesk.audit.service.AuditLogService;
import com.saadaoui.tunidesk.category.dto.CategoryRequest;
import com.saadaoui.tunidesk.category.dto.CategoryResponse;
import com.saadaoui.tunidesk.category.mapper.CategoryMapper;
import com.saadaoui.tunidesk.category.repository.CategoryRepository;
import com.saadaoui.tunidesk.category.service.CategoryService;
import com.saadaoui.tunidesk.entity.TicketCategory;
import com.saadaoui.tunidesk.exception.DuplicateResourceException;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import com.saadaoui.tunidesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private final TicketRepository ticketRepository;

    private final CategoryMapper categoryMapper;

    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {

        if (categoryRepository.existsByName(request.getName().trim())) {
            throw new DuplicateResourceException("Category name already exists");
        }

        TicketCategory category = categoryMapper.toEntity(request);

        TicketCategory saved = categoryRepository.save(category);

        log.info("Category created: {} (id={})", saved.getName(), saved.getId());

        auditLogService.record(AuditConstants.ACTION_CREATE, AuditConstants.ENTITY_TICKET_CATEGORY,
                saved.getId(), "Created ticket category \"" + saved.getName() + "\"");

        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {

        TicketCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (categoryRepository.existsByNameAndIdNot(request.getName().trim(), id)) {
            throw new DuplicateResourceException("Category name already exists");
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setActive(request.getActive());

        TicketCategory saved = categoryRepository.save(category);

        log.info("Category updated: {} (id={})", saved.getName(), saved.getId());

        auditLogService.record(AuditConstants.ACTION_UPDATE, AuditConstants.ENTITY_TICKET_CATEGORY,
                saved.getId(), "Updated ticket category \"" + saved.getName() + "\"");

        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {

        return categoryRepository.findById(id)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(Boolean activeOnly) {

        List<TicketCategory> categories = categoryRepository.findAllByOrderByNameAsc();

        if (Boolean.TRUE.equals(activeOnly)) {
            categories = categories.stream()
                    .filter(TicketCategory::getActive)
                    .toList();
        }

        return categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        TicketCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (ticketRepository.existsByCategory(category)) {
            throw new IllegalArgumentException(
                    "Category cannot be deleted because tickets are linked to it");
        }

        categoryRepository.delete(category);

        log.info("Category deleted: {} (id={})", category.getName(), id);

        auditLogService.record(AuditConstants.ACTION_DELETE, AuditConstants.ENTITY_TICKET_CATEGORY,
                id, "Deleted ticket category \"" + category.getName() + "\"");
    }

}
