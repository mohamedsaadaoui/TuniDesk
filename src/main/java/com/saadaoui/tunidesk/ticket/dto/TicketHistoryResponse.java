package com.saadaoui.tunidesk.ticket.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TicketHistoryResponse {

    private UUID id;

    private String actor;

    private String action;

    private String fromStatus;

    private String toStatus;

    private String details;

    private LocalDateTime createdAt;

}
