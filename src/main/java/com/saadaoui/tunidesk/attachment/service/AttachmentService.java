package com.saadaoui.tunidesk.attachment.service;

import com.saadaoui.tunidesk.attachment.dto.TicketAttachmentResponse;
import com.saadaoui.tunidesk.entity.Ticket;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AttachmentService {

    TicketAttachmentResponse upload(UUID ticketId, MultipartFile file, String actorEmail);

    List<TicketAttachmentResponse> listByTicket(UUID ticketId, String actorEmail);

    DownloadedFile download(UUID attachmentId, String actorEmail);

    void delete(UUID attachmentId, String actorEmail);

    void deleteAllForTicket(Ticket ticket);

    record DownloadedFile(byte[] content, String contentType, String fileName) {
    }

}
