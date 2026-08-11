package com.saadaoui.tunidesk.attachment.repository;

import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, UUID> {

    List<TicketAttachment> findByTicketOrderByCreatedAtAsc(Ticket ticket);

}
