package com.saadaoui.tunidesk.sla.service.impl;

import com.saadaoui.tunidesk.audit.AuditConstants;
import com.saadaoui.tunidesk.audit.service.AuditLogService;
import com.saadaoui.tunidesk.entity.SlaPolicy;
import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketPriority;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import com.saadaoui.tunidesk.sla.dto.SlaPolicyRequest;
import com.saadaoui.tunidesk.sla.dto.SlaPolicyResponse;
import com.saadaoui.tunidesk.sla.mapper.SlaPolicyMapper;
import com.saadaoui.tunidesk.sla.repository.SlaPolicyRepository;
import com.saadaoui.tunidesk.sla.service.SlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private final SlaPolicyRepository slaPolicyRepository;

    private final SlaPolicyMapper slaPolicyMapper;

    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime computeDueAt(TicketPriority priority) {

        SlaPolicy policy = findPolicy(priority);

        return LocalDateTime.now().plusHours(policy.getHours());
    }

    @Override
    @Transactional
    public void applyDueAt(Ticket ticket) {

        SlaPolicy policy = findPolicy(ticket.getPriority());

        ticket.setSlaDueAt(LocalDateTime.now().plusHours(policy.getHours()));
        ticket.setSlaBreached(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> getAllPolicies() {

        return slaPolicyRepository.findAllByOrderByHoursAsc()
                .stream()
                .map(slaPolicyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SlaPolicyResponse updatePolicy(UUID id, SlaPolicyRequest request) {

        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found"));

        policy.setName(request.getName());
        policy.setHours(request.getHours());

        SlaPolicy saved = slaPolicyRepository.save(policy);

        auditLogService.record(AuditConstants.ACTION_UPDATE, AuditConstants.ENTITY_SLA_POLICY,
                saved.getId(), "Updated SLA policy \"" + saved.getPriority() + "\" to " + saved.getHours() + "h");

        return slaPolicyMapper.toResponse(saved);
    }

    private SlaPolicy findPolicy(TicketPriority priority) {

        return slaPolicyRepository.findByPriority(priority)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No SLA policy configured for priority " + priority));
    }

}
