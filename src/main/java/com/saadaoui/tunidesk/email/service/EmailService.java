package com.saadaoui.tunidesk.email.service;

import com.saadaoui.tunidesk.auth.entity.User;

import java.util.List;

public interface EmailService {

    void sendTicketAlert(String subject, String text, List<User> recipients);

    void sendPasswordResetEmail(User user, String resetLink);

}
