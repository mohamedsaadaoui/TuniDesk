package com.saadaoui.tunidesk.attachment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TicketAttachmentResponse {

    private UUID id;

    private String originalName;

    private String contentType;

    private Long size;

    private UUID uploadedById;

    private String uploadedByFirstName;

    private String uploadedByLastName;

    private LocalDateTime createdAt;

}
