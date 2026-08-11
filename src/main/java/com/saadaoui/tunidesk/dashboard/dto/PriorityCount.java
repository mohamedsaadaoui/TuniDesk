package com.saadaoui.tunidesk.dashboard.dto;

import com.saadaoui.tunidesk.entity.TicketPriority;

public record PriorityCount(TicketPriority priority, long count) {
}
