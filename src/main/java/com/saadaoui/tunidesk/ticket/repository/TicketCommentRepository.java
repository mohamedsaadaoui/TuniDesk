package com.saadaoui.tunidesk.ticket.repository;

import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {

    List<TicketComment> findByTicketOrderByCreatedAtAsc(Ticket ticket);

    long countByTicket(Ticket ticket);

}
