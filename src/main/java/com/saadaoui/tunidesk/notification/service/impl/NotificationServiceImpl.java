package com.saadaoui.tunidesk.notification.service.impl;

import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.auth.repository.UserRepository;
import com.saadaoui.tunidesk.entity.Notification;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import com.saadaoui.tunidesk.notification.NotificationTypes;
import com.saadaoui.tunidesk.notification.dto.NotificationRequest;
import com.saadaoui.tunidesk.notification.dto.NotificationResponse;
import com.saadaoui.tunidesk.notification.mapper.NotificationMapper;
import com.saadaoui.tunidesk.notification.repository.NotificationRepository;
import com.saadaoui.tunidesk.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;

    private final NotificationMapper notificationMapper;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public NotificationResponse send(NotificationRequest request, String senderEmail) {

        User sender = findUserByEmail(senderEmail);
        User recipient = userRepository.findById(request.recipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

        Notification notification = Notification.builder()
                .sender(sender)
                .recipient(recipient)
                .title(request.title())
                .message(request.message())
                .type(NotificationTypes.TICKET_CREATED)
                .build();

        Notification saved = notificationRepository.save(notification);

        NotificationResponse response = notificationMapper.toResponse(saved);

        push(response);

        return response;
    }

    @Override
    @Transactional
    public void notifyUser(User recipient, String type, String title, String message) {

        notifyUsers(List.of(recipient), type, title, message);
    }

    @Override
    @Transactional
    public void notifyUsers(List<User> recipients, String type, String title, String message) {

        recipients.stream()
                .distinct()
                .filter(User::getEnabled)
                .forEach(recipient -> {

                    Notification notification = Notification.builder()
                            .recipient(recipient)
                            .title(title)
                            .message(message)
                            .type(type)
                            .build();

                    Notification saved = notificationRepository.save(notification);

                    push(notificationMapper.toResponse(saved));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(String recipientEmail, Boolean unreadOnly, Pageable pageable) {

        User recipient = findUserByEmail(recipientEmail);

        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository
                    .findByRecipientAndIsReadOrderByCreatedAtDesc(recipient, false, pageable)
                    .map(notificationMapper::toResponse);
        }

        return notificationRepository
                .findByRecipientOrderByCreatedAtDesc(recipient, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientEmail) {

        User recipient = findUserByEmail(recipientEmail);

        return notificationRepository.countByRecipientAndIsReadFalse(recipient);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID id, String recipientEmail) {

        User recipient = findUserByEmail(recipientEmail);

        Notification notification = notificationRepository.findByIdAndRecipient(id, recipient)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setIsRead(true);

        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllRead(String recipientEmail) {

        User recipient = findUserByEmail(recipientEmail);

        notificationRepository.markAllRead(recipient);
    }

    @Override
    @Transactional
    public void delete(UUID id, String recipientEmail) {

        User recipient = findUserByEmail(recipientEmail);

        Notification notification = notificationRepository.findByIdAndRecipient(id, recipient)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notificationRepository.delete(notification);
    }

    private User findUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void push(NotificationResponse response) {

        try {
            messagingTemplate.convertAndSendToUser(
                    response.getRecipientId().toString(),
                    "/queue/notifications",
                    response);
        } catch (RuntimeException ignored) {
            // WebSocket delivery must never break the notification write
        }
    }

}
