package com.saadaoui.tunidesk.attachment.controller;

import com.saadaoui.tunidesk.attachment.dto.TicketAttachmentResponse;
import com.saadaoui.tunidesk.attachment.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(value = "/api/tickets/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketAttachmentResponse> upload(
            @PathVariable UUID ticketId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(attachmentService.upload(ticketId, file, currentUser.getUsername()));
    }

    @GetMapping("/api/tickets/{ticketId}/attachments")
    public List<TicketAttachmentResponse> list(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UserDetails currentUser) {

        return attachmentService.listByTicket(ticketId, currentUser.getUsername());
    }

    @GetMapping("/api/attachments/{id}/download")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {

        AttachmentService.DownloadedFile downloaded =
                attachmentService.download(id, currentUser.getUsername());

        String encodedName = URLEncoder.encode(downloaded.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloaded.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + downloaded.fileName() + "\"; filename*=UTF-8''" + encodedName)
                .body(new ByteArrayResource(downloaded.content()));
    }

    @DeleteMapping("/api/attachments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {

        attachmentService.delete(id, currentUser.getUsername());
    }

}
