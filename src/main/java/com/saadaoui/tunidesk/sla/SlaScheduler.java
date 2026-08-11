package com.saadaoui.tunidesk.sla;

import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketStatus;
import com.saadaoui.tunidesk.notification.NotificationTypes;
import com.saadaoui.tunidesk.notification.service.NotificationService;
import com.saadaoui.tunidesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaScheduler {

    private static final long APPROACHING_WINDOW_HOURS = 1;

    private static final List<TicketStatus> SLA_STOPPED = List.of(
            TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;

    private final NotificationService notificationService;

    @Value("${application.sla.check-cron}")
    private String checkCron;

    @Scheduled(cron = "${application.sla.check-cron}")
    @Transactional
    public void checkSla() {

        LocalDateTime now = LocalDateTime.now();

        detectBreaches(now);

        detectApproaching(now);
    }

    private void detectBreaches(LocalDateTime now) {

        List<Ticket> breached = ticketRepository
                .findByStatusNotInAndSlaDueAtBefore(SLA_STOPPED, now)
                .stream()
                .filter(ticket -> !Boolean.TRUE.equals(ticket.getSlaBreached()))
                .toList();

        for (Ticket ticket : breached) {

            ticket.setSlaBreached(true);

            ticketRepository.save(ticket);

            notifyParties(ticket, NotificationTypes.SLA_BREACHED,
                    "SLA breached for " + ticket.getTicketNumber(),
                    "\"" + ticket.getTitle() + "\" exceeded its SLA deadline (" + ticket.getSlaDueAt() + ")");

            log.warn("SLA breached for ticket {}", ticket.getTicketNumber());
        }
    }

    private void detectApproaching(LocalDateTime now) {

        List<Ticket> approaching = ticketRepository
                .findByStatusNotInAndSlaApproachingNotifiedFalseAndSlaDueAtBetween(
                        SLA_STOPPED, now, now.plusHours(APPROACHING_WINDOW_HOURS));

        for (Ticket ticket : approaching) {

            ticket.setSlaApproachingNotified(true);

            ticketRepository.save(ticket);

            notifyParties(ticket, NotificationTypes.SLA_APPROACHING,
                    "SLA approaching for " + ticket.getTicketNumber(),
                    "\"" + ticket.getTitle() + "\" must be resolved before " + ticket.getSlaDueAt());
        }
    }

    private void notifyParties(Ticket ticket, String type, String title, String message) {

        List<User> recipients = new ArrayList<>();

        if (ticket.getAssignedTo() != null) {
            recipients.add(ticket.getAssignedTo());
        }
        recipients.add(ticket.getCreatedBy());

        notificationService.notifyUsers(recipients, type, title, message);
    }

}
