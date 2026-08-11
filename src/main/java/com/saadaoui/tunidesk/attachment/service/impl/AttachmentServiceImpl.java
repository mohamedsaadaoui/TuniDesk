package com.saadaoui.tunidesk.attachment.service.impl;

import com.saadaoui.tunidesk.attachment.dto.TicketAttachmentResponse;
import com.saadaoui.tunidesk.attachment.mapper.TicketAttachmentMapper;
import com.saadaoui.tunidesk.attachment.repository.TicketAttachmentRepository;
import com.saadaoui.tunidesk.attachment.service.AttachmentService;
import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.auth.repository.UserRepository;
import com.saadaoui.tunidesk.entity.Ticket;
import com.saadaoui.tunidesk.entity.TicketAttachment;
import com.saadaoui.tunidesk.exception.AccessDeniedException;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import com.saadaoui.tunidesk.ticket.repository.TicketRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final String ROLE_AGENT = "AGENT";
    private static final String ROLE_ADMIN = "ADMIN";

    private final TicketAttachmentRepository attachmentRepository;

    private final TicketRepository ticketRepository;

    private final UserRepository userRepository;

    private final TicketAttachmentMapper attachmentMapper;

    @Value("${application.upload.dir:./uploads}")
    private String uploadDir;

    @PostConstruct
    void init() {

        try {
            Files.createDirectories(Path.of(uploadDir));
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot create upload directory: " + uploadDir, ex);
        }
    }

    @Override
    @Transactional
    public TicketAttachmentResponse upload(UUID ticketId, MultipartFile file, String actorEmail) {

        User actor = findUser(actorEmail);
        Ticket ticket = findTicket(ticketId);

        requireAccess(ticket, actor);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File must be at most 10 MB");
        }

        String originalName = sanitize(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + originalName;

        try (var input = file.getInputStream()) {
            Files.copy(input, Path.of(uploadDir, storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store uploaded file", ex);
        }

        TicketAttachment attachment = TicketAttachment.builder()
                .ticket(ticket)
                .originalName(originalName)
                .storedName(storedName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(actor)
                .build();

        TicketAttachment saved = attachmentRepository.save(attachment);

        log.info("Attachment '{}' ({} bytes) uploaded to ticket {} by {}",
                saved.getOriginalName(), saved.getSize(), ticket.getTicketNumber(), actor.getEmail());

        return attachmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> listByTicket(UUID ticketId, String actorEmail) {

        User actor = findUser(actorEmail);
        Ticket ticket = findTicket(ticketId);

        requireAccess(ticket, actor);

        return attachmentRepository.findByTicketOrderByCreatedAtAsc(ticket)
                .stream()
                .map(attachmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedFile download(UUID attachmentId, String actorEmail) {

        User actor = findUser(actorEmail);

        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        requireAccess(attachment.getTicket(), actor);

        Path file = Path.of(uploadDir, attachment.getStoredName());

        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("Attachment file is missing on disk");
        }

        try {
            return new DownloadedFile(
                    Files.readAllBytes(file),
                    attachment.getContentType() == null ? "application/octet-stream" : attachment.getContentType(),
                    attachment.getOriginalName());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read attachment from disk", ex);
        }
    }

    @Override
    @Transactional
    public void delete(UUID attachmentId, String actorEmail) {

        User actor = findUser(actorEmail);

        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        boolean agent = isAgent(actor);
        boolean uploader = attachment.getUploadedBy().getId().equals(actor.getId());

        if (!agent && !uploader) {
            throw new AccessDeniedException("Only the uploader, AGENT or ADMIN can delete an attachment");
        }

        try {
            Files.deleteIfExists(Path.of(uploadDir, attachment.getStoredName()));
        } catch (IOException ex) {
            log.warn("Failed to delete attachment file {}: {}", attachment.getStoredName(), ex.getMessage());
        }

        attachmentRepository.delete(attachment);

        log.info("Attachment '{}' deleted from ticket {} by {}",
                attachment.getOriginalName(), attachment.getTicket().getTicketNumber(), actor.getEmail());
    }

    @Override
    @Transactional
    public void deleteAllForTicket(Ticket ticket) {

        for (TicketAttachment attachment : attachmentRepository.findByTicketOrderByCreatedAtAsc(ticket)) {

            try {
                Files.deleteIfExists(Path.of(uploadDir, attachment.getStoredName()));
            } catch (IOException ex) {
                log.warn("Failed to delete attachment file {}: {}", attachment.getStoredName(), ex.getMessage());
            }

            attachmentRepository.delete(attachment);
        }
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

    private String sanitize(String name) {

        if (name == null || name.isBlank()) {
            return "file";
        }

        String cleaned = Path.of(name).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");

        return cleaned.isBlank() ? "file" : cleaned;
    }

}
