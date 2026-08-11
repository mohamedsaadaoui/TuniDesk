package com.saadaoui.tunidesk.sla.dto;

import com.saadaoui.tunidesk.entity.TicketPriority;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SlaPolicyResponse {

    private UUID id;

    private String name;

    private TicketPriority priority;

    private Integer hours;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
