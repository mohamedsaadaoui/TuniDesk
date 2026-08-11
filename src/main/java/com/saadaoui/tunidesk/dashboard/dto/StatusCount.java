package com.saadaoui.tunidesk.dashboard.dto;

import com.saadaoui.tunidesk.entity.TicketPriority;
import com.saadaoui.tunidesk.entity.TicketStatus;

public record StatusCount(TicketStatus status, long count) {
}
