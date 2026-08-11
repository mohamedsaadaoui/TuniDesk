package com.saadaoui.tunidesk.email.service.impl;

import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.email.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${application.email.enabled:false}")
    private boolean enabled;

    @Value("${application.email.from:no-reply@tunidesk.local}")
    private String from;

    @Value("${application.email.base-url:http://localhost:5173}")
    private String baseUrl;

    @Override
    @Async
    public void sendTicketAlert(String subject, String text, List<User> recipients) {

        if (!enabled) {
            return;
        }

        List<String> addresses = recipients.stream()
                .filter(User::getEnabled)
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (addresses.isEmpty()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(addresses.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(buildHtml(text), true);

            mailSender.send(message);

            log.info("Email sent to {} recipient(s): {}", addresses.size(), subject);
        } catch (RuntimeException | MessagingException ex) {
            log.warn("Failed to send email '{}': {}", subject, ex.getMessage());
        }
    }

    private String buildHtml(String text) {

        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        String paragraphs = escaped.replace("\n", "<br/>");

        return """
                <!DOCTYPE html>
                <html lang="en">
                <body style="margin:0;padding:0;background:#f4f5f7;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f5f7;padding:24px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="560" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:10px;overflow:hidden;border:1px solid #e3e6ea;">
                          <tr>
                            <td style="background:#0f3d6e;padding:18px 28px;">
                              <span style="color:#ffffff;font-size:18px;font-weight:bold;">TuniDesk</span>
                              <span style="color:#9fc2e8;font-size:13px;"> &mdash; HelpDesk</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;color:#33383f;font-size:14px;line-height:1.6;">
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 28px;border-top:1px solid #eef0f3;color:#8a919b;font-size:12px;">
                              <a href="%s" style="color:#0f3d6e;">Open TuniDesk</a>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(paragraphs, baseUrl);
    }
}
