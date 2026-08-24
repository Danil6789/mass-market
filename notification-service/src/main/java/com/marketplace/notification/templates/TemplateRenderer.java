package com.marketplace.notification.templates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static com.marketplace.notification.constant.ExceptionMessages.TEMPLATE_NOT_FOUND;

/**
 * Loads Thymeleaf templates from the {@code notification_template} table and
 * evaluates their subject and body as inline Thymeleaf fragments against a
 * per-event variable map.
 *
 * <p>The {@link TemplateEngine} used here is configured with a
 * {@code StringTemplateResolver} (see {@code TemplateEngineConfig}) so that
 * template <em>strings</em> (rather than template <em>names</em>) are
 * processed.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateRenderer {

    private final TemplateRepository templateRepository;
    private final TemplateEngine stringTemplateEngine;

    /**
     * Render the template identified by {@code eventType}.
     *
     * @param eventType key into {@code notification_template.event_type}
     * @param model     variables substituted into {@code [[${var}]]} expressions
     * @return the rendered subject and body
     * @throws IllegalStateException if no template is registered for the event type
     */
    public Rendered render(String eventType, Map<String, Object> model) {
        NotificationTemplate template = templateRepository.findByEventType(eventType)
                .orElseThrow(() -> {
                    log.error("No notification template registered for eventType={}", eventType);
                    return new IllegalStateException(TEMPLATE_NOT_FOUND + ": " + eventType);
                });

        Context context = new Context();
        context.setVariables(model);

        String subject = stringTemplateEngine.process(template.getSubjectTemplate(), context);
        String body = stringTemplateEngine.process(template.getBodyTemplate(), context);
        return new Rendered(subject, body);
    }

    /**
     * Result of rendering: a fully resolved (subject, body) pair.
     */
    public record Rendered(String subject, String body) {
    }
}
