package com.saadaoui.tunidesk.ticket.controller;

import com.saadaoui.tunidesk.entity.TicketPriority;
import com.saadaoui.tunidesk.entity.TicketStatus;
import com.saadaoui.tunidesk.ticket.TicketCsvExporter;
import com.saadaoui.tunidesk.ticket.dto.*;
import com.saadaoui.tunidesk.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketService.create(request, currentUser.getUsername()));
    }

    @PutMapping("/{id}")
    public TicketResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody TicketUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ticketService.update(id, request, currentUser.getUsername());
    }

    @GetMapping("/{id}")
    public TicketDetailResponse getDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ticketService.getDetail(id, currentUser.getUsername());
    }

    @GetMapping
    public Page<TicketResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID assignedToId,
            @RequestParam(required = false) Boolean slaBreached,
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ticketService.getAll(search, status, priority, categoryId, assignedToId,
                slaBreached, currentUser.getUsername(), createPageable(page, size, sortBy, direction));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID assignedToId,
            @RequestParam(required = false) Boolean slaBreached,
            @AuthenticationPrincipal UserDetails currentUser) {

        String csv = TicketCsvExporter.export(ticketService.getForExport(search, status, priority,
                categoryId, assignedToId, slaBreached, currentUser.getUsername()));

        String fileName = "tickets-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ticketService.assign(id, request, currentUser.getUsername());
    }

    @PostMapping("/{id}/take")
    public TicketResponse take(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ticketService.take(id, currentUser.getUsername());
    }

    @PostMapping("/{id}/status")
    public TicketResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusChangeRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ticketService.changeStatus(id, request, currentUser.getUsername());
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketCommentResponse> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketService.addComment(id, request, currentUser.getUsername()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {

        ticketService.delete(id, currentUser.getUsername());
    }

    private Pageable createPageable(int page, int size, String sortBy, String direction) {

        Sort.Direction sortDirection =
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

}
