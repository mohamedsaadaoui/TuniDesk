package com.saadaoui.tunidesk.ticket.dto;

import com.saadaoui.tunidesk.attachment.dto.TicketAttachmentResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TicketDetailResponse {

    private TicketResponse ticket;

    private List<TicketCommentResponse> comments;

    private List<TicketHistoryResponse> history;

    private List<TicketAttachmentResponse> attachments;

}
