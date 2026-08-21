package com.saadaoui.tunidesk.ticket.service.impl;

import com.saadaoui.tunidesk.attachment.mapper.TicketAttachmentMapper;
import com.saadaoui.tunidesk.attachment.repository.TicketAttachmentRepository;
import com.saadaoui.tunidesk.attachment.service.AttachmentService;
import com.saadaoui.tunidesk.audit.AuditConstants;
import com.saadaoui.tunidesk.audit.service.AuditLogService;
import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.auth.repository.UserRepository;
import com.saadaoui.tunidesk.category.repository.CategoryRepository;
import com.saadaoui.tunidesk.email.service.EmailService;
import com.saadaoui.tunidesk.entity.*;
import com.saadaoui.tunidesk.exception.AccessDeniedException;
import com.saadaoui.tunidesk.exception.InvalidStatusTransitionException;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import com.saadaoui.tunidesk.notification.NotificationTypes;
import com.saadaoui.tunidesk.notification.service.NotificationService;
import com.saadaoui.tunidesk.sla.service.SlaService;
import com.saadaoui.tunidesk.ticket.TicketHistoryActions;
import com.saadaoui.tunidesk.ticket.dto.*;
import com.saadaoui.tunidesk.ticket.mapper.TicketMapper;
import com.saadaoui.tunidesk.ticket.repository.TicketCommentRepository;
import com.saadaoui.tunidesk.ticket.repository.TicketHistoryRepository;
import com.saadaoui.tunidesk.ticket.repository.TicketRepository;
import com.saadaoui.tunidesk.ticket.service.TicketService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENT = "AGENT";

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = Map.of(
            TicketStatus.OPEN, EnumSet.of(TicketStatus.ASSIGNED),
            TicketStatus.ASSIGNED, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.OPEN),
            TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED),
            TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED, TicketStatus.OPEN),
            TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class)
    );

    private static final List<TicketStatus> SLA_STOPPED = List.of(
            TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;

    private final TicketCommentRepository commentRepository;

    private final TicketHistoryRepository historyRepository;

    private final TicketAttachmentRepository attachmentRepository;

    private final TicketAttachmentMapper attachmentMapper;

    private final AttachmentService attachmentService;

    private final CategoryRepository categoryRepository;

    private final UserRepository userRepository;

    private final TicketMapper ticketMapper;

    private final SlaService slaService;

    private final NotificationService notificationService;

    private final EmailService emailService;

    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public TicketResponse create(TicketRequest request, String actorEmail) {

        User actor = findUser(actorEmail);

        TicketCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new IllegalArgumentException("Cannot create a ticket in an inactive category");
        }

        Ticket ticket = Ticket.builder()
                .ticketNumber(nextTicketNumber())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(TicketStatus.OPEN)
                .createdBy(actor)
                .category(category)
                .build();

        slaService.applyDueAt(ticket);

        Ticket saved = ticketRepository.save(ticket);

        recordHistory(saved, TicketHistoryActions.CREATED, null, null,
                "Ticket created by " + actor.getEmail());

        log.info("Ticket created: {} (id={})", saved.getTicketNumber(), saved.getId());

        auditLogService.record(AuditConstants.ACTION_CREATE, AuditConstants.ENTITY_TICKET,
                saved.getId(), "Created ticket " + saved.getTicketNumber() + " \"" + saved.getTitle() + "\"");

        if (request.getPriority() == TicketPriority.HIGH || request.getPriority() == TicketPriority.CRITICAL) {
            notifyAgents(NotificationTypes.TICKET_CREATED,
                    "New " + request.getPriority() + " ticket " + saved.getTicketNumber(),
                    "\"" + saved.getTitle() + "\" was submitted and needs attention");
        }

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse update(UUID id, TicketUpdateRequest request, String actorEmail) {

        User actor = findUser(actorEmail);

        requireAgent(actor, "Only AGENT or ADMIN can edit tickets");

        Ticket ticket = findTicket(id);

        TicketCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        boolean priorityChanged = ticket.getPriority() != request.getPriority();

        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription());
        ticket.setCategory(category);

        if (priorityChanged) {
            ticket.setPriority(request.getPriority());
            if (!SLA_STOPPED.contains(ticket.getStatus())) {
                slaService.applyDueAt(ticket);
            }
        }

        Ticket saved = ticketRepository.save(ticket);

        recordHistory(saved, TicketHistoryActions.UPDATED, null, null,
                "Ticket updated by " + actor.getEmail());

        auditLogService.record(AuditConstants.ACTION_UPDATE, AuditConstants.ENTITY_TICKET,
                saved.getId(), "Updated ticket " + saved.getTicketNumber());

        notifyUsersExcept(actor, List.of(saved.getCreatedBy()),
                NotificationTypes.TICKET_STATUS_CHANGED,
                "Ticket " + saved.getTicketNumber() + " updated",
                "\"" + saved.getTitle() + "\" was updated");

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailResponse getDetail(UUID id, String actorEmail) {

        User actor = findUser(actorEmail);

        Ticket ticket = findTicket(id);

        requireAccess(ticket, actor);

        boolean agent = isAgent(actor);

        List<TicketCommentResponse> comments = commentRepository
                .findByTicketOrderByCreatedAtAsc(ticket)
                .stream()
                .filter(comment -> agent || !Boolean.TRUE.equals(comment.getIsInternal()))
                .map(ticketMapper::toCommentResponse)
                .toList();

        List<TicketHistoryResponse> history = historyRepository
                .findByTicketOrderByCreatedAtAsc(ticket)
                .stream()
                .map(ticketMapper::toHistoryResponse)
                .toList();

        List<com.saadaoui.tunidesk.attachment.dto.TicketAttachmentResponse> attachments =
                attachmentRepository.findByTicketOrderByCreatedAtAsc(ticket)
                        .stream()
                        .map(attachmentMapper::toResponse)
                        .toList();

        return TicketDetailResponse.builder()
                .ticket(ticketMapper.toResponse(ticket))
                .comments(comments)
                .history(history)
                .attachments(attachments)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getAll(String search, TicketStatus status, TicketPriority priority,
                                       UUID categoryId, UUID assignedToId, Boolean slaBreached,
                                       String actorEmail, Pageable pageable) {

        User actor = findUser(actorEmail);

        Specification<Ticket> spec = buildSpecification(search, status, priority,
                categoryId, assignedToId, slaBreached, actor);

        return ticketRepository.findAll(spec, pageable)
                .map(ticketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getForExport(String search, TicketStatus status, TicketPriority priority,
                                             UUID categoryId, UUID assignedToId, Boolean slaBreached,
                                             String actorEmail) {

        User actor = findUser(actorEmail);

        Specification<Ticket> spec = buildSpecification(search, status, priority,
                categoryId, assignedToId, slaBreached, actor);

        return ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse assign(UUID id, AssignRequest request, String actorEmail) {

        User actor = findUser(actorEmail);

        requireAgent(actor, "Only AGENT or ADMIN can assign tickets");

        Ticket ticket = findTicket(id);

        if (request.getAssignedToId() == null) {
            return unassign(ticket, actor);
        }

        User assignee = userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        if (!isAgent(assignee)) {
            throw new IllegalArgumentException("Only AGENT or ADMIN can be assigned to a ticket");
        }

        ticket.setAssignedTo(assignee);
        ticket.setStatus(TicketStatus.ASSIGNED);

        Ticket saved = ticketRepository.save(ticket);

        recordHistory(saved, TicketHistoryActions.ASSIGNED, null, null,
                "Assigned to " + assignee.getEmail() + (request.getNote() == null ? "" : " - " + request.getNote()));

        log.info("Ticket {} assigned to {}", saved.getTicketNumber(), assignee.getEmail());

        auditLogService.record(AuditConstants.ACTION_UPDATE, AuditConstants.ENTITY_TICKET,
                saved.getId(), "Assigned ticket " + saved.getTicketNumber() + " to " + assignee.getEmail());

        notificationService.notifyUsers(List.of(assignee, saved.getCreatedBy()),
                NotificationTypes.TICKET_ASSIGNED,
                "Ticket " + saved.getTicketNumber() + " assigned",
                "\"" + saved.getTitle() + "\" was assigned to " + assignee.getFirstName() + " " + assignee.getLastName());

        emailService.sendTicketAlert(
                "Ticket " + saved.getTicketNumber() + " assigned to you",
                "\"" + saved.getTitle() + "\" was assigned to you by "
                        + actor.getFirstName() + " " + actor.getLastName() + "\n\n"
                        + "Priority: " + saved.getPriority(),
                List.of(assignee));

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse take(UUID id, String actorEmail) {

        User actor = findUser(actorEmail);

        requireAgent(actor, "Only AGENT or ADMIN can take a ticket");

        Ticket ticket = findTicket(id);

        ticket.setAssignedTo(actor);
        ticket.setStatus(TicketStatus.ASSIGNED);

        Ticket saved = ticketRepository.save(ticket);

        recordHistory(saved, TicketHistoryActions.ASSIGNED, null, null,
                "Ticket taken by " + actor.getEmail());

        notificationService.notifyUsers(List.of(saved.getCreatedBy()),
                NotificationTypes.TICKET_ASSIGNED,
                "Ticket " + saved.getTicketNumber() + " assigned",
                "\"" + saved.getTitle() + "\" was taken by " + actor.getFirstName() + " " + actor.getLastName());

        emailService.sendTicketAlert(
                "Your ticket " + saved.getTicketNumber() + " has been taken",
                "\"" + saved.getTitle() + "\" was taken by "
                        + actor.getFirstName() + " " + actor.getLastName(),
                List.of(saved.getCreatedBy()));

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse changeStatus(UUID id, StatusChangeRequest request, String actorEmail) {

        User actor = findUser(actorEmail);

        Ticket ticket = findTicket(id);

        requireAccess(ticket, actor);

        TicketStatus current = ticket.getStatus();
        TicketStatus target = request.getStatus();

        Set<TicketStatus> allowed = TRANSITIONS.getOrDefault(current, Collections.emptySet());

        if (!allowed.contains(target)) {
            throw new InvalidStatusTransitionException(
                    "Cannot change ticket status from " + current + " to " + target);
        }

        if (!isAgent(actor)) {
            boolean creator = ticket.getCreatedBy().getId().equals(actor.getId());
            if (!creator) {
                throw new AccessDeniedException("Only the ticket creator can close or reopen their own ticket");
            }
            if (target != TicketStatus.CLOSED && target != TicketStatus.OPEN) {
                throw new AccessDeniedException("Only AGENT or ADMIN can move the ticket through the workflow");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        if (target == TicketStatus.ASSIGNED && ticket.getAssignedTo() == null) {
            requireAgent(actor, "Assign the ticket before marking it as ASSIGNED");
            ticket.setAssignedTo(actor);
        }

        if (target == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(now);
            ticket.setClosedAt(null);
            ticket.setSlaBreached(false);
            ticket.setSlaApproachingNotified(false);
        }

        if (target == TicketStatus.CLOSED) {
            ticket.setClosedAt(now);
            ticket.setSlaBreached(false);
        }

        if (target == TicketStatus.OPEN && current == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(null);
            ticket.setClosedAt(null);
            ticket.setAssignedTo(null);
            ticket.setSlaApproachingNotified(false);
            slaService.applyDueAt(ticket);
        }

        ticket.setStatus(target);

        Ticket saved = ticketRepository.save(ticket);

        String details = request.getComment() == null
                ? "Status changed from " + current + " to " + target + " by " + actor.getEmail()
                : "Status changed from " + current + " to " + target + " by " + actor.getEmail()
                + " - " + request.getComment();

        recordHistory(saved,
                target == TicketStatus.OPEN && current == TicketStatus.RESOLVED
                        ? TicketHistoryActions.REOPENED : TicketHistoryActions.STATUS_CHANGED,
                current.name(), target.name(), details);

        log.info("Ticket {} status {} -> {} by {}", saved.getTicketNumber(), current, target, actor.getEmail());

        auditLogService.record(AuditConstants.ACTION_UPDATE, AuditConstants.ENTITY_TICKET,
                saved.getId(), "Ticket " + saved.getTicketNumber() + " " + current + " -> " + target);

        notifyStatusChange(saved, current, target, actor);

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketCommentResponse addComment(UUID id, CommentRequest request, String actorEmail) {

        User actor = findUser(actorEmail);

        Ticket ticket = findTicket(id);

        requireAccess(ticket, actor);

        boolean internal = Boolean.TRUE.equals(request.getIsInternal());

        if (internal && !isAgent(actor)) {
            throw new AccessDeniedException("Only AGENT or ADMIN can add internal comments");
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author(actor)
                .content(request.getContent().trim())
                .isInternal(internal)
                .build();

        TicketComment saved = commentRepository.save(comment);

        recordHistory(ticket, TicketHistoryActions.COMMENTED, null, null,
                (internal ? "Internal comment: " : "Comment: ") + saved.getContent());

        List<User> recipients = new ArrayList<>();
        if (!ticket.getCreatedBy().getId().equals(actor.getId())) {
            recipients.add(ticket.getCreatedBy());
        }
        if (ticket.getAssignedTo() != null && !ticket.getAssignedTo().getId().equals(actor.getId())) {
            recipients.add(ticket.getAssignedTo());
        }

        notificationService.notifyUsers(recipients,
                NotificationTypes.TICKET_COMMENTED,
                "New comment on " + ticket.getTicketNumber(),
                actor.getFirstName() + " commented: " + truncated(saved.getContent()));

        emailService.sendTicketAlert(
                "New comment on " + ticket.getTicketNumber(),
                actor.getFirstName() + " " + actor.getLastName() + " commented on \""
                        + ticket.getTitle() + "\":\n\n" + saved.getContent(),
                recipients);

        return ticketMapper.toCommentResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id, String actorEmail) {

        User actor = findUser(actorEmail);

        if (!actor.getRoles().stream().anyMatch(role -> role.getName().equals(ROLE_ADMIN))) {
            throw new AccessDeniedException("Only ADMIN can delete tickets");
        }

        Ticket ticket = findTicket(id);

        attachmentService.deleteAllForTicket(ticket);
        commentRepository.deleteAll(commentRepository.findByTicketOrderByCreatedAtAsc(ticket));
        historyRepository.deleteAll(historyRepository.findByTicketOrderByCreatedAtAsc(ticket));

        ticketRepository.delete(ticket);

        log.info("Ticket deleted: {} (id={})", ticket.getTicketNumber(), id);

        auditLogService.record(AuditConstants.ACTION_DELETE, AuditConstants.ENTITY_TICKET,
                id, "Deleted ticket " + ticket.getTicketNumber());
    }

    private TicketResponse unassign(Ticket ticket, User actor) {

        ticket.setAssignedTo(null);
        if (ticket.getStatus() == TicketStatus.ASSIGNED) {
            ticket.setStatus(TicketStatus.OPEN);
        }

        Ticket saved = ticketRepository.save(ticket);

        recordHistory(saved, TicketHistoryActions.UNASSIGNED, null, null,
                "Unassigned by " + actor.getEmail());

        notificationService.notifyUsers(List.of(saved.getCreatedBy()),
                NotificationTypes.TICKET_STATUS_CHANGED,
                "Ticket " + saved.getTicketNumber() + " unassigned",
                "\"" + saved.getTitle() + "\" is no longer assigned");

        emailService.sendTicketAlert(
                "Ticket " + saved.getTicketNumber() + " unassigned",
                "\"" + saved.getTitle() + "\" is no longer assigned to anyone.",
                List.of(saved.getCreatedBy()));

        return ticketMapper.toResponse(saved);
    }

    private void notifyStatusChange(Ticket ticket, TicketStatus from, TicketStatus to, User actor) {

        String title = "Ticket " + ticket.getTicketNumber() + " " + from + " -> " + to;
        String message = "\"" + ticket.getTitle() + "\" was moved from " + from + " to " + to
                + " by " + actor.getFirstName() + " " + actor.getLastName();

        List<User> recipients = new ArrayList<>();

        if (!ticket.getCreatedBy().getId().equals(actor.getId())) {
            recipients.add(ticket.getCreatedBy());
        }
        if (ticket.getAssignedTo() != null && !ticket.getAssignedTo().getId().equals(actor.getId())) {
            recipients.add(ticket.getAssignedTo());
        }

        notificationService.notifyUsers(recipients, NotificationTypes.TICKET_STATUS_CHANGED, title, message);

        emailService.sendTicketAlert(
                "Ticket " + ticket.getTicketNumber() + ": " + from + " -> " + to,
                "\"" + ticket.getTitle() + "\" was moved from " + from + " to " + to
                        + " by " + actor.getFirstName() + " " + actor.getLastName(),
                recipients);
    }

    private void notifyAgents(String type, String title, String message) {

        List<User> agents = userRepository.findByRolesNameAndEnabledTrue(ROLE_AGENT);

        notificationService.notifyUsers(agents, type, title, message);
    }

    private void notifyUsersExcept(User actor, List<User> recipients, String type, String title, String message) {

        List<User> filtered = recipients.stream()
                .filter(user -> !user.getId().equals(actor.getId()))
                .toList();

        notificationService.notifyUsers(filtered, type, title, message);
    }

    private void recordHistory(Ticket ticket, String action, String from, String to, String details) {

        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .actor(currentActor())
                .action(action)
                .fromStatus(from)
                .toStatus(to)
                .details(details)
                .build();

        historyRepository.save(history);
    }

    private String currentActor() {

        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }

        return "system";
    }

    private void requireAccess(Ticket ticket, User actor) {

        if (isAgent(actor)) {
            return;
        }

        boolean creator = ticket.getCreatedBy().getId().equals(actor.getId());
        boolean assignee = ticket.getAssignedTo() != null
                && ticket.getAssignedTo().getId().equals(actor.getId());

        if (!creator && !assignee) {
            throw new AccessDeniedException("You do not have access to this ticket");
        }
    }

    private void requireAgent(User actor, String message) {

        if (!isAgent(actor)) {
            throw new AccessDeniedException(message);
        }
    }

    private boolean isAgent(User user) {

        return user.getRoles().stream().anyMatch(role ->
                role.getName().equals(ROLE_AGENT) || role.getName().equals(ROLE_ADMIN));
    }

    private User findUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Ticket findTicket(UUID id) {

        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private synchronized String nextTicketNumber() {

        int max = ticketRepository.findAllTicketNumbers().stream()
                .map(this::extractSequence)
                .max(Integer::compareTo)
                .orElse(0);

        return String.format("TD-%04d", max + 1);
    }

    private int extractSequence(String ticketNumber) {

        try {
            return Integer.parseInt(ticketNumber.replaceAll("\\D", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String truncated(String value) {

        if (value == null || value.length() <= 80) {
            return value;
        }
        return value.substring(0, 80) + "...";
    }

    private Specification<Ticket> buildSpecification(String search, TicketStatus status,
                                                     TicketPriority priority, UUID categoryId,
                                                     UUID assignedToId, Boolean slaBreached,
                                                     User actor) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("ticketNumber")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (assignedToId != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedToId));
            }

            if (Boolean.TRUE.equals(slaBreached)) {
                predicates.add(cb.isTrue(root.get("slaBreached")));
                predicates.add(cb.not(root.get("status").in(SLA_STOPPED)));
            }

            if (!isAgent(actor)) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), actor.getId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
