package com.saadaoui.tunidesk.ticket.dto;

import com.saadaoui.tunidesk.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StatusChangeRequest {

    @NotNull(message = "Target status is required")
    private TicketStatus status;

    @Size(max = 500, message = "Comment must be at most 500 characters")
    private String comment;

}
