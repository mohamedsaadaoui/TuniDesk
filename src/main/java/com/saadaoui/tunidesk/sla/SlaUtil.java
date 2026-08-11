package com.saadaoui.tunidesk.sla;

import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public final class SlaUtil {

    private SlaUtil() {
    }

    public static boolean isSlaStopped(TicketStatus status) {

        return status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED;
    }

    public static boolean isBreached(Ticket ticket, LocalDateTime now) {

        return ticket.getSlaDueAt() != null
                && !isSlaStopped(ticket.getStatus())
                && now.isAfter(ticket.getSlaDueAt());
    }

    public static Duration remaining(Ticket ticket, LocalDateTime now) {

        if (ticket.getSlaDueAt() == null || isSlaStopped(ticket.getStatus())) {
            return Duration.ZERO;
        }

        return Duration.between(now, ticket.getSlaDueAt());
    }

    public static String formatRemaining(Duration duration) {

        long totalMinutes = Math.max(0, duration.toMinutes());
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        return String.format("%02dh %02dm", hours, minutes);
    }

}
