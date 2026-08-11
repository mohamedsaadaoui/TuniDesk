package com.saadaoui.tunidesk.sla.service;

import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketPriority;
import com.saadaoui.tunidesk.sla.dto.SlaPolicyRequest;
import com.saadaoui.tunidesk.sla.dto.SlaPolicyResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SlaService {

    LocalDateTime computeDueAt(TicketPriority priority);

    void applyDueAt(Ticket ticket);

    List<SlaPolicyResponse> getAllPolicies();

    SlaPolicyResponse updatePolicy(UUID id, SlaPolicyRequest request);

}
