package com.marketplace.notification.email;

import com.marketplace.notification.config.MailProperties;
import com.marketplace.notification.templates.TemplateRenderer;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.marketplace.notification.constant.ExceptionMessages.EMAIL_SEND_FAILED;

/**
 * Renders a Thymeleaf template, builds an HTML {@link MimeMessage} and
 * dispatches it through {@link JavaMailSender}. Every attempt (success or
 * failure) is recorded in {@link EmailLog}.
 *
 * <p>Mail failures are re-thrown so the surrounding {@code @RetryableTopic}
 * consumer can retry / route to DLT.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateRenderer templateRenderer;
    private final EmailLogRepository emailLogRepository;
    private final MailProperties mailProperties;

    /**
     * Render the template identified by {@code eventType} against {@code model},
     * send the resulting HTML email to {@code recipient}, and persist a log row.
     *
     * @throws MailException on SMTP / MIME failures (caller retries / DLT)
     */
    @Transactional
    public void send(String eventType, String recipient, Map<String, Object> model) {
        TemplateRenderer.Rendered rendered = templateRenderer.render(eventType, model);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(recipient);
            helper.setSubject(rendered.subject());
            helper.setText(rendered.body(), true);

            mailSender.send(message);
            emailLogRepository.save(EmailLog.success(recipient, rendered.subject(), rendered.body()));
            log.info("Email sent: eventType={}, recipient={}", eventType, recipient);
        } catch (Exception ex) {
            log.error("Failed to send email: eventType={}, recipient={}: {}",
                    eventType, recipient, ex.getMessage(), ex);
            emailLogRepository.save(EmailLog.failure(recipient, rendered.subject(), rendered.body(), ex.getMessage()));
            throw new MailSendException(EMAIL_SEND_FAILED + ": " + ex.getMessage(), ex);
        }
    }
}
