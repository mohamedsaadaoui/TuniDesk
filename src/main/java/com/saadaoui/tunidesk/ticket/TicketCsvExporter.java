package com.saadaoui.tunidesk.ticket;

import com.saadaoui.tunidesk.ticket.dto.TicketResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class TicketCsvExporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String SEPARATOR = ",";

    private static final String NEWLINE = "\r\n";

    private static final String QUOTE = "\"";

    private TicketCsvExporter() {
    }

    public static String export(List<TicketResponse> tickets) {

        StringBuilder csv = new StringBuilder();

        appendRow(csv, "Ticket Number", "Title", "Status", "Priority", "Category",
                "Creator", "Assignee", "SLA Breached", "SLA Due At",
                "Created At", "Resolved At", "Closed At");

        for (TicketResponse t : tickets) {
            appendRow(csv,
                    t.getTicketNumber(),
                    t.getTitle(),
                    String.valueOf(t.getStatus()),
                    String.valueOf(t.getPriority()),
                    t.getCategoryName(),
                    fullName(t.getCreatedByFirstName(), t.getCreatedByLastName(), t.getCreatedByEmail()),
                    fullName(t.getAssignedToFirstName(), t.getAssignedToLastName(), t.getAssignedToEmail()),
                    Boolean.TRUE.equals(t.getSlaBreached()) ? "yes" : "no",
                    formatDate(t.getSlaDueAt()),
                    formatDate(t.getCreatedAt()),
                    formatDate(t.getResolvedAt()),
                    formatDate(t.getClosedAt()));
        }

        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, String... cells) {

        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                csv.append(SEPARATOR);
            }
            csv.append(QUOTE).append(escape(cells[i])).append(QUOTE);
        }

        csv.append(NEWLINE);
    }

    private static String escape(String value) {

        return value == null ? "" : value.replace(QUOTE, QUOTE + QUOTE);
    }

    private static String fullName(String firstName, String lastName, String email) {

        if (firstName == null && lastName == null) {
            return "";
        }
        return (firstName + " " + lastName).trim() + " <" + email + ">";
    }

    private static String formatDate(LocalDateTime value) {

        return value == null ? "" : DATE_FORMAT.format(value);
    }
}
