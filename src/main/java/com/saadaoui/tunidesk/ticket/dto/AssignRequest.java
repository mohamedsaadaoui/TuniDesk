package com.saadaoui.tunidesk.ticket.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignRequest {

    private UUID assignedToId;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;

}
