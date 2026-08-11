package com.saadaoui.tunidesk.attachment.mapper;

import com.saadaoui.tunidesk.attachment.dto.TicketAttachmentResponse;
import com.saadaoui.tunidesk.entity.TicketAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketAttachmentMapper {

    @Mapping(target = "uploadedById", source = "uploadedBy.id")
    @Mapping(target = "uploadedByFirstName", source = "uploadedBy.firstName")
    @Mapping(target = "uploadedByLastName", source = "uploadedBy.lastName")
    TicketAttachmentResponse toResponse(TicketAttachment attachment);

}
