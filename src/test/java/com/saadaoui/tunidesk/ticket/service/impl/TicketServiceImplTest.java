package com.saadaoui.tunidesk.ticket.service.impl;

import com.saadaoui.tunidesk.attachment.mapper.TicketAttachmentMapper;
import com.saadaoui.tunidesk.attachment.repository.TicketAttachmentRepository;
import com.saadaoui.tunidesk.attachment.service.AttachmentService;
import com.saadaoui.tunidesk.audit.service.AuditLogService;
import com.saadaoui.tunidesk.auth.entity.Role;
import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.auth.repository.UserRepository;
import com.saadaoui.tunidesk.category.repository.CategoryRepository;
import com.saadaoui.tunidesk.email.service.EmailService;
import com.saadaoui.tunidesk.entity.*;
import com.saadaoui.tunidesk.exception.AccessDeniedException;
import com.saadaoui.tunidesk.exception.InvalidStatusTransitionException;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import com.saadaoui.tunidesk.notification.service.NotificationService;
import com.saadaoui.tunidesk.sla.service.SlaService;
import com.saadaoui.tunidesk.ticket.dto.StatusChangeRequest;
import com.saadaoui.tunidesk.ticket.dto.TicketRequest;
import com.saadaoui.tunidesk.ticket.dto.TicketResponse;
import com.saadaoui.tunidesk.ticket.mapper.TicketMapper;
import com.saadaoui.tunidesk.ticket.repository.TicketCommentRepository;
import com.saadaoui.tunidesk.ticket.repository.TicketHistoryRepository;
import com.saadaoui.tunidesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository commentRepository;

    @Mock
    private TicketHistoryRepository historyRepository;

    @Mock
    private TicketAttachmentRepository attachmentRepository;

    @Mock
    private TicketAttachmentMapper attachmentMapper;

    @Mock
    private AttachmentService attachmentService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private SlaService slaService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    private TicketServiceImpl service;

    private User agent;

    private User admin;

    private User regularUser;

    private TicketCategory category;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(
                ticketRepository, commentRepository, historyRepository,
                attachmentRepository, attachmentMapper, attachmentService,
                categoryRepository, userRepository, ticketMapper,
                slaService, notificationService, emailService, auditLogService);

        Role agentRole = Role.builder().name("AGENT").build();
        Role adminRole = Role.builder().name("ADMIN").build();
        Role userRole = Role.builder().name("USER").build();

        agent = User.builder().id(UUID.randomUUID()).firstName("Support").lastName("Agent")
                .email("agent@test.com").roles(Set.of(agentRole)).build();
        admin = User.builder().id(UUID.randomUUID()).firstName("Admin").lastName("TuniDesk")
                .email("admin@test.com").roles(Set.of(adminRole)).build();
        regularUser = User.builder().id(UUID.randomUUID()).firstName("Demo").lastName("Employee")
                .email("user@test.com").roles(Set.of(userRole)).build();

        category = TicketCategory.builder().id(UUID.randomUUID()).name("Software").active(true).build();
    }

    @Test
    void create_shouldSetOpenStatusApplySlaAndReturnResponse() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(ticketRepository.findAllTicketNumbers()).thenReturn(List.of());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenReturn(TicketResponse.builder().build());

        TicketRequest request = new TicketRequest();
        request.setTitle("  Laptop not booting  ");
        request.setDescription("Screen stays black");
        request.setPriority(TicketPriority.MEDIUM);
        request.setCategoryId(category.getId());

        service.create(request, "user@test.com");

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());

        Ticket saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(saved.getTitle()).isEqualTo("Laptop not booting");
        assertThat(saved.getTicketNumber()).startsWith("TD-");
        assertThat(saved.getCreatedBy()).isEqualTo(regularUser);
        verify(slaService).applyDueAt(saved);
        verify(historyRepository).save(any(TicketHistory.class));
    }

    @Test
    void create_highPriority_shouldNotifyAgents() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(ticketRepository.findAllTicketNumbers()).thenReturn(List.of());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRolesNameAndEnabledTrue("AGENT")).thenReturn(List.of(agent));
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenReturn(TicketResponse.builder().build());

        TicketRequest request = new TicketRequest();
        request.setTitle("Critical outage");
        request.setPriority(TicketPriority.CRITICAL);
        request.setCategoryId(category.getId());

        service.create(request, "user@test.com");

        verify(notificationService).notifyUsers(
                eq(List.of(agent)), anyString(), anyString(), anyString());
    }

    @Test
    void create_inactiveCategory_shouldThrow() {
        TicketCategory inactive = TicketCategory.builder()
                .id(UUID.randomUUID()).name("Archived").active(false).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(categoryRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        TicketRequest request = new TicketRequest();
        request.setTitle("Test");
        request.setPriority(TicketPriority.LOW);
        request.setCategoryId(inactive.getId());

        assertThatThrownBy(() -> service.create(request, "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void changeStatus_validTransition_shouldMoveTicket() {
        Ticket ticket = openTicket();

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenReturn(TicketResponse.builder().build());

        StatusChangeRequest request = new StatusChangeRequest();
        request.setStatus(TicketStatus.ASSIGNED);
        request.setComment("Taking this");

        service.changeStatus(ticket.getId(), request, "agent@test.com");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(ticket.getAssignedTo()).isEqualTo(agent);
    }

    @Test
    void changeStatus_toResolved_shouldSetResolvedAt() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .ticketNumber("TD-0001")
                .title("Bug")
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.IN_PROGRESS)
                .createdBy(regularUser)
                .assignedTo(agent)
                .category(category)
                .build();

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenReturn(TicketResponse.builder().build());

        StatusChangeRequest request = new StatusChangeRequest();
        request.setStatus(TicketStatus.RESOLVED);

        service.changeStatus(ticket.getId(), request, "agent@test.com");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolvedAt()).isNotNull();
        assertThat(ticket.getSlaBreached()).isFalse();
    }

    @Test
    void changeStatus_invalidTransition_shouldThrow() {
        Ticket ticket = openTicket();

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusChangeRequest request = new StatusChangeRequest();
        request.setStatus(TicketStatus.CLOSED);

        assertThatThrownBy(() -> service.changeStatus(ticket.getId(), request, "agent@test.com"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void changeStatus_nonCreatorUser_shouldBeDenied() {
        User other = User.builder().id(UUID.randomUUID()).firstName("Other").lastName("User")
                .email("other@test.com").roles(Set.of(Role.builder().name("USER").build())).build();

        Ticket ticket = openTicket();

        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(other));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusChangeRequest request = new StatusChangeRequest();
        request.setStatus(TicketStatus.CLOSED);

        assertThatThrownBy(() -> service.changeStatus(ticket.getId(), request, "other@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void changeStatus_creatorClosingOwnTicket_shouldBeAllowed() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .ticketNumber("TD-0001")
                .title("Bug")
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.RESOLVED)
                .createdBy(regularUser)
                .category(category)
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenReturn(TicketResponse.builder().build());

        StatusChangeRequest request = new StatusChangeRequest();
        request.setStatus(TicketStatus.CLOSED);

        service.changeStatus(ticket.getId(), request, "user@test.com");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    void assign_nonAgent_shouldBeDenied() {
        Ticket ticket = openTicket();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        com.saadaoui.tunidesk.ticket.dto.AssignRequest request =
                new com.saadaoui.tunidesk.ticket.dto.AssignRequest();
        request.setAssignedToId(agent.getId());

        assertThatThrownBy(() -> service.assign(ticket.getId(), request, "user@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assign_success_shouldSetAssignedStatus() {
        Ticket ticket = openTicket();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenReturn(TicketResponse.builder().build());

        com.saadaoui.tunidesk.ticket.dto.AssignRequest request =
                new com.saadaoui.tunidesk.ticket.dto.AssignRequest();
        request.setAssignedToId(agent.getId());

        service.assign(ticket.getId(), request, "admin@test.com");

        assertThat(ticket.getAssignedTo()).isEqualTo(agent);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
    }

    @Test
    void assign_toNonAgent_shouldThrow() {
        Ticket ticket = openTicket();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        com.saadaoui.tunidesk.ticket.dto.AssignRequest request =
                new com.saadaoui.tunidesk.ticket.dto.AssignRequest();
        request.setAssignedToId(regularUser.getId());

        assertThatThrownBy(() -> service.assign(ticket.getId(), request, "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getDetail_nonCreatorUser_shouldBeDenied() {
        Ticket ticket = openTicket();

        when(userRepository.findByEmail("other@test.com"))
                .thenReturn(Optional.of(User.builder()
                        .id(UUID.randomUUID()).firstName("Other").lastName("User")
                        .email("other@test.com")
                        .roles(Set.of(Role.builder().name("USER").build()))
                        .build()));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getDetail(ticket.getId(), "other@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getDetail_ticketNotFound_shouldThrow() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(ticketRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(UUID.randomUUID(), "admin@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_nonAdmin_shouldBeDenied() {
        Ticket ticket = openTicket();

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.delete(ticket.getId(), "agent@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void delete_admin_shouldDelete() {
        Ticket ticket = openTicket();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicketOrderByCreatedAtAsc(ticket)).thenReturn(List.of());
        when(historyRepository.findByTicketOrderByCreatedAtAsc(ticket)).thenReturn(List.of());

        service.delete(ticket.getId(), "admin@test.com");

        verify(attachmentService).deleteAllForTicket(ticket);
        verify(ticketRepository).delete(ticket);
    }

    private Ticket openTicket() {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .ticketNumber("TD-0001")
                .title("Bug")
                .description("It is broken")
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .createdBy(regularUser)
                .category(category)
                .build();
    }
}
