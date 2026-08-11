package com.saadaoui.tunidesk.ticket.repository;

import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketCategory;
import com.saadaoui.tunidesk.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    boolean existsByCategory(TicketCategory category);

    long countByStatus(TicketStatus status);

    long countByStatusNotIn(Collection<TicketStatus> statuses);

    long countBySlaBreachedTrueAndStatusNotIn(Collection<TicketStatus> statuses);

    List<Ticket> findTop5ByOrderByCreatedAtDesc();

    Page<Ticket> findByCreatedByOrderByCreatedAtDesc(User createdBy, Pageable pageable);

    List<Ticket> findByStatusNotInAndSlaDueAtBefore(Collection<TicketStatus> statuses, LocalDateTime before);

    List<Ticket> findByStatusNotInAndSlaBreachedFalse(Collection<TicketStatus> statuses);

    List<Ticket> findByStatusNotInAndSlaBreachedTrue(Collection<TicketStatus> statuses);

    List<Ticket> findByStatusNotInAndSlaApproachingNotifiedFalseAndSlaDueAtBetween(
            Collection<TicketStatus> statuses, LocalDateTime from, LocalDateTime to);

    @Query("SELECT t.ticketNumber FROM Ticket t")
    List<String> findAllTicketNumbers();

    long countByAssignedToIsNull();

    @Query("SELECT t.priority, COUNT(t) FROM Ticket t GROUP BY t.priority")
    List<Object[]> countGroupByPriority();

    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT t.category.name, COUNT(t) FROM Ticket t GROUP BY t.category.name")
    List<Object[]> countGroupByCategoryName();

    @Query("SELECT t.createdAt, t.resolvedAt FROM Ticket t WHERE t.resolvedAt IS NOT NULL")
    List<Object[]> findResolutionWindows();

}
