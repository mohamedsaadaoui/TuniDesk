package com.saadaoui.tunidesk.category.repository;

import com.saadaoui.tunidesk.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<TicketCategory, UUID> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    List<TicketCategory> findAllByOrderByNameAsc();

}
