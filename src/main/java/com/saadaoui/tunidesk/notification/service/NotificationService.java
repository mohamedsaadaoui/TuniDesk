package com.saadaoui.tunidesk.notification.service;

import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.notification.dto.NotificationRequest;
import com.saadaoui.tunidesk.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse send(NotificationRequest request, String senderEmail);

    void notifyUser(User recipient, String type, String title, String message);

    void notifyUsers(List<User> recipients, String type, String title, String message);

    Page<NotificationResponse> getMyNotifications(String recipientEmail, Boolean unreadOnly, Pageable pageable);

    long getUnreadCount(String recipientEmail);

    NotificationResponse markAsRead(UUID id, String recipientEmail);

    void markAllRead(String recipientEmail);

    void delete(UUID id, String recipientEmail);

}
