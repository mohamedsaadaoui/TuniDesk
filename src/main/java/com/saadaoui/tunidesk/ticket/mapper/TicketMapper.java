package com.saadaoui.tunidesk.ticket.mapper;

import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketComment;
import com.saadaoui.tunidesk.entity.TicketHistory;
import com.saadaoui.tunidesk.sla.SlaUtil;
import com.saadaoui.tunidesk.ticket.dto.TicketCommentResponse;
import com.saadaoui.tunidesk.ticket.dto.TicketHistoryResponse;
import com.saadaoui.tunidesk.ticket.dto.TicketResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Duration;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByFirstName", source = "createdBy.firstName")
    @Mapping(target = "createdByLastName", source = "createdBy.lastName")
    @Mapping(target = "createdByEmail", source = "createdBy.email")
    @Mapping(target = "assignedToId", source = "assignedTo.id")
    @Mapping(target = "assignedToFirstName", source = "assignedTo.firstName")
    @Mapping(target = "assignedToLastName", source = "assignedTo.lastName")
    @Mapping(target = "assignedToEmail", source = "assignedTo.email")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "slaBreached", ignore = true)
    @Mapping(target = "slaTimeRemaining", ignore = true)
    @Mapping(target = "slaRemainingMinutes", ignore = true)
    TicketResponse toResponse(Ticket ticket);

    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorFirstName", source = "author.firstName")
    @Mapping(target = "authorLastName", source = "author.lastName")
    @Mapping(target = "authorEmail", source = "author.email")
    TicketCommentResponse toCommentResponse(TicketComment comment);

    @Mapping(target = "id", source = "id")
    TicketHistoryResponse toHistoryResponse(TicketHistory history);

    @AfterMapping
    default void computeSla(Ticket ticket, @MappingTarget TicketResponse response) {

        LocalDateTime now = LocalDateTime.now();

        boolean breached = SlaUtil.isBreached(ticket, now);

        response.setSlaBreached(breached);

        if (ticket.getSlaDueAt() == null || SlaUtil.isSlaStopped(ticket.getStatus())) {
            response.setSlaTimeRemaining(null);
            response.setSlaRemainingMinutes(null);
            return;
        }

        Duration remaining = SlaUtil.remaining(ticket, now);

        if (breached || remaining.isZero()) {
            response.setSlaTimeRemaining("Expired");
            response.setSlaRemainingMinutes(0L);
        } else {
            response.setSlaTimeRemaining(SlaUtil.formatRemaining(remaining));
            response.setSlaRemainingMinutes(remaining.toMinutes());
        }
    }

}
