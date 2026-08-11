package com.saadaoui.tunidesk.ticket.dto;

import com.saadaoui.tunidesk.entity.TicketPriority;
import com.saadaoui.tunidesk.entity.TicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TicketResponse {

    private UUID id;

    private String ticketNumber;

    private String title;

    private String description;

    private TicketPriority priority;

    private TicketStatus status;

    private UUID createdById;

    private String createdByFirstName;

    private String createdByLastName;

    private String createdByEmail;

    private UUID assignedToId;

    private String assignedToFirstName;

    private String assignedToLastName;

    private String assignedToEmail;

    private UUID categoryId;

    private String categoryName;

    private LocalDateTime slaDueAt;

    private Boolean slaBreached;

    private String slaTimeRemaining;

    private Long slaRemainingMinutes;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
