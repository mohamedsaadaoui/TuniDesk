package com.saadaoui.tunidesk.dashboard.dto;

import com.saadaoui.tunidesk.ticket.dto.TicketResponse;

import java.util.List;

public record DashboardSummaryResponse(
        long totalTickets,
        long openTickets,
        long inProgressTickets,
        long resolvedTickets,
        long closedTickets,
        long unassignedTickets,
        long slaBreachedTickets,
        Double avgResolutionHours,
        List<StatusCount> ticketsByStatus,
        List<PriorityCount> ticketsByPriority,
        List<CategoryTicketCount> ticketsByCategory,
        List<TicketResponse> recentTickets
) {
}
