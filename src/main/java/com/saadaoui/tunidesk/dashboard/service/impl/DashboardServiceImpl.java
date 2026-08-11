package com.saadaoui.tunidesk.dashboard.service.impl;

import com.saadaoui.tunidesk.dashboard.dto.*;
import com.saadaoui.tunidesk.dashboard.service.DashboardService;
import com.saadaoui.tunidesk.entity.TicketPriority;
import com.saadaoui.tunidesk.entity.TicketStatus;
import com.saadaoui.tunidesk.ticket.mapper.TicketMapper;
import com.saadaoui.tunidesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final List<TicketStatus> CLOSED_STATES = List.of(TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;

    private final TicketMapper ticketMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(String actorEmail) {

        long totalTickets = ticketRepository.count();

        Map<TicketStatus, Long> byStatus = countByStatus();

        Map<TicketPriority, Long> byPriority = countByPriority();

        List<CategoryTicketCount> byCategory = countByCategory();

        return new DashboardSummaryResponse(
                totalTickets,
                byStatus.getOrDefault(TicketStatus.OPEN, 0L),
                byStatus.getOrDefault(TicketStatus.IN_PROGRESS, 0L),
                byStatus.getOrDefault(TicketStatus.RESOLVED, 0L),
                byStatus.getOrDefault(TicketStatus.CLOSED, 0L),
                ticketRepository.countByAssignedToIsNull(),
                ticketRepository.countBySlaBreachedTrueAndStatusNotIn(CLOSED_STATES),
                averageResolutionHours(),
                toStatusCounts(byStatus),
                toPriorityCounts(byPriority),
                byCategory,
                ticketRepository.findTop5ByOrderByCreatedAtDesc()
                        .stream()
                        .map(ticketMapper::toResponse)
                        .toList()
        );
    }

    private Map<TicketStatus, Long> countByStatus() {

        Map<TicketStatus, Long> counts = new HashMap<>();

        ticketRepository.countGroupByStatus().forEach(row -> {

            TicketStatus status = (TicketStatus) row[0];

            Long count = (Long) row[1];

            counts.put(status, count);
        });

        return counts;
    }

    private Map<TicketPriority, Long> countByPriority() {

        Map<TicketPriority, Long> counts = new HashMap<>();

        ticketRepository.countGroupByPriority().forEach(row -> {

            TicketPriority priority = (TicketPriority) row[0];

            Long count = (Long) row[1];

            counts.put(priority, count);
        });

        return counts;
    }

    private List<CategoryTicketCount> countByCategory() {

        return ticketRepository.countGroupByCategoryName().stream()
                .map(row -> new CategoryTicketCount((String) row[0], (Long) row[1]))
                .toList();
    }

    private List<StatusCount> toStatusCounts(Map<TicketStatus, Long> counts) {

        List<StatusCount> result = new ArrayList<>();

        for (TicketStatus status : TicketStatus.values()) {
            result.add(new StatusCount(status, counts.getOrDefault(status, 0L)));
        }

        return result;
    }

    private List<PriorityCount> toPriorityCounts(Map<TicketPriority, Long> counts) {

        List<PriorityCount> result = new ArrayList<>();

        for (TicketPriority priority : TicketPriority.values()) {
            result.add(new PriorityCount(priority, counts.getOrDefault(priority, 0L)));
        }

        return result;
    }

    private Double averageResolutionHours() {

        List<Object[]> windows = ticketRepository.findResolutionWindows();

        if (windows.isEmpty()) {
            return null;
        }

        double totalSeconds = windows.stream()
                .mapToDouble(row -> Duration.between(
                        (LocalDateTime) row[0],
                        (LocalDateTime) row[1]).toSeconds())
                .sum();

        return totalSeconds / windows.size() / 3600.0;
    }

}
