package com.saadaoui.tunidesk.ticket.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TicketCommentResponse {

    private UUID id;

    private UUID authorId;

    private String authorFirstName;

    private String authorLastName;

    private String authorEmail;

    private String content;

    private Boolean isInternal;

    private LocalDateTime createdAt;

}
