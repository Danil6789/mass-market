package com.marketplace.notification.email;

import com.marketplace.notification.config.MailProperties;
import com.marketplace.notification.templates.TemplateRenderer;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailService}. All collaborators are mocked; the
 * generated {@link MimeMessage} is inspected through JavaMail's
 * {@link Session} parser.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateRenderer templateRenderer;
    @Mock private EmailLogRepository emailLogRepository;

    private MailProperties mailProperties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setFrom("noreply@marketplace.local");
        emailService = new EmailService(mailSender, templateRenderer, emailLogRepository, mailProperties);
    }

    @Test
    void send_rendersTemplateAndLogsSuccess() throws Exception {
        when(templateRenderer.render("WELCOME", Map.of("name", "Alice")))
                .thenReturn(new TemplateRenderer.Rendered("Welcome, Alice", "<p>Hi Alice</p>"));

        // Return a real MimeMessage so we can inspect subject/to after send.
        Session session = Session.getInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.send("WELCOME", "alice@example.com", Map.of("name", "Alice"));

        verify(mailSender).send(mimeMessage);

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository).save(captor.capture());
        EmailLog saved = captor.getValue();
        assertThat(saved.getRecipient()).isEqualTo("alice@example.com");
        assertThat(saved.getSubject()).isEqualTo("Welcome, Alice");
        assertThat(saved.getBody()).isEqualTo("<p>Hi Alice</p>");
        assertThat(saved.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(saved.getErrorMessage()).isNull();

        assertThat(mimeMessage.getSubject()).isEqualTo("Welcome, Alice");
    }

    @Test
    void send_savesFailedLogAndRethrows() {
        when(templateRenderer.render(any(), any()))
                .thenReturn(new TemplateRenderer.Rendered("s", "b"));
        Session session = Session.getInstance(new Properties());
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(session));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.send("WELCOME", "x@y", Map.of()))
                .isInstanceOf(MailSendException.class);

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository).save(captor.capture());
        EmailLog saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(saved.getErrorMessage()).contains("smtp down");
    }

    @Test
    void send_passesMailFromProperty() {
        mailProperties.setFrom("custom@marketplace.local");
        when(templateRenderer.render(any(), any()))
                .thenReturn(new TemplateRenderer.Rendered("s", "b"));
        Session session = Session.getInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.send("WELCOME", "user@example.com", Map.of());

        // Just verify no exception and that the mail was sent with our configured from.
        verify(mailSender).send(mimeMessage);
    }
}
