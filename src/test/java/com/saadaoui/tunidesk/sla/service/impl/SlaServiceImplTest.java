package com.saadaoui.tunidesk.sla.service.impl;

import com.saadaoui.tunidesk.audit.service.AuditLogService;
import com.saadaoui.tunidesk.entity.SlaPolicy;
import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketPriority;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import com.saadaoui.tunidesk.sla.dto.SlaPolicyRequest;
import com.saadaoui.tunidesk.sla.dto.SlaPolicyResponse;
import com.saadaoui.tunidesk.sla.mapper.SlaPolicyMapper;
import com.saadaoui.tunidesk.sla.repository.SlaPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaServiceImplTest {

    @Mock
    private SlaPolicyRepository slaPolicyRepository;

    @Mock
    private SlaPolicyMapper slaPolicyMapper;

    @Mock
    private AuditLogService auditLogService;

    private SlaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SlaServiceImpl(slaPolicyRepository, slaPolicyMapper, auditLogService);
    }

    @Test
    void computeDueAt_shouldUsePolicyHours() {
        SlaPolicy policy = SlaPolicy.builder()
                .id(UUID.randomUUID()).name("High SLA").priority(TicketPriority.HIGH).hours(8).build();

        when(slaPolicyRepository.findByPriority(TicketPriority.HIGH)).thenReturn(Optional.of(policy));

        LocalDateTime before = LocalDateTime.now();
        LocalDateTime due = service.computeDueAt(TicketPriority.HIGH);

        assertThat(due).isAfterOrEqualTo(before.plusHours(8).minusSeconds(1));
        assertThat(due).isBeforeOrEqualTo(LocalDateTime.now().plusHours(8).plusSeconds(1));
    }

    @Test
    void computeDueAt_missingPolicy_shouldThrow() {
        when(slaPolicyRepository.findByPriority(TicketPriority.LOW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.computeDueAt(TicketPriority.LOW))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void applyDueAt_shouldSetDueAndResetBreach() {
        SlaPolicy policy = SlaPolicy.builder()
                .id(UUID.randomUUID()).name("Critical SLA").priority(TicketPriority.CRITICAL).hours(4).build();

        when(slaPolicyRepository.findByPriority(TicketPriority.CRITICAL)).thenReturn(Optional.of(policy));

        Ticket ticket = Ticket.builder().priority(TicketPriority.CRITICAL).slaBreached(true).build();

        service.applyDueAt(ticket);

        assertThat(ticket.getSlaDueAt()).isNotNull();
        assertThat(ticket.getSlaBreached()).isFalse();
    }

    @Test
    void updatePolicy_shouldPersistChanges() {
        SlaPolicy policy = SlaPolicy.builder()
                .id(UUID.randomUUID()).name("Old").priority(TicketPriority.MEDIUM).hours(24).build();

        when(slaPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(slaPolicyRepository.save(any(SlaPolicy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(slaPolicyMapper.toResponse(any(SlaPolicy.class)))
                .thenReturn(SlaPolicyResponse.builder().build());

        SlaPolicyRequest request = new SlaPolicyRequest();
        request.setName("New name");
        request.setHours(12);

        service.updatePolicy(policy.getId(), request);

        assertThat(policy.getName()).isEqualTo("New name");
        assertThat(policy.getHours()).isEqualTo(12);
        verify(auditLogService).record(anyString(), anyString(), any(UUID.class), anyString());
    }
}
