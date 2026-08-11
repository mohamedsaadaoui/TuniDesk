package com.saadaoui.tunidesk.ticket.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TicketDetailResponse {

    private TicketResponse ticket;

    private List<TicketCommentResponse> comments;

    private List<TicketHistoryResponse> history;

}
