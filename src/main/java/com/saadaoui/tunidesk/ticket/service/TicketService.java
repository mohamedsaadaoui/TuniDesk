package com.saadaoui.tunidesk.ticket.service;

import com.saadaoui.tunidesk.entity.TicketPriority;
import com.saadaoui.tunidesk.entity.TicketStatus;
import com.saadaoui.tunidesk.ticket.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    TicketResponse create(TicketRequest request, String actorEmail);

    TicketResponse update(UUID id, TicketUpdateRequest request, String actorEmail);

    TicketDetailResponse getDetail(UUID id, String actorEmail);

    Page<TicketResponse> getAll(String search, TicketStatus status, TicketPriority priority,
                                UUID categoryId, UUID assignedToId, Boolean slaBreached,
                                String actorEmail, Pageable pageable);

    List<TicketResponse> getForExport(String search, TicketStatus status, TicketPriority priority,
                                      UUID categoryId, UUID assignedToId, Boolean slaBreached,
                                      String actorEmail);

    TicketResponse assign(UUID id, AssignRequest request, String actorEmail);

    TicketResponse take(UUID id, String actorEmail);

    TicketResponse changeStatus(UUID id, StatusChangeRequest request, String actorEmail);

    TicketCommentResponse addComment(UUID id, CommentRequest request, String actorEmail);

    void delete(UUID id, String actorEmail);

}
