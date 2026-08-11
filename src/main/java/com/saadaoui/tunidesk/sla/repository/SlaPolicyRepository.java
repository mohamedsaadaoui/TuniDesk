package com.saadaoui.tunidesk.sla.repository;

import com.saadaoui.tunidesk.entity.SlaPolicy;
import com.saadaoui.tunidesk.entity.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {

    Optional<SlaPolicy> findByPriority(TicketPriority priority);

    List<SlaPolicy> findAllByOrderByHoursAsc();

    boolean existsByPriority(TicketPriority priority);

    boolean existsByPriorityAndIdNot(TicketPriority priority, UUID id);

}
