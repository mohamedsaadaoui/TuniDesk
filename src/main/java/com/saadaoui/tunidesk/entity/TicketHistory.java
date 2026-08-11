package com.saadaoui.tunidesk.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Ticket is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @NotBlank(message = "Actor is required")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String actor;

    @NotBlank(message = "Action is required")
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String action;

    @Size(max = 50)
    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Size(max = 50)
    @Column(name = "to_status", length = 50)
    private String toStatus;

    @Size(max = 500)
    @Column(length = 500)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
