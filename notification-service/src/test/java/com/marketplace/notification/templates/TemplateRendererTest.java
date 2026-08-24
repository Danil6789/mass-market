package com.marketplace.notification.templates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemplateRenderer}. The {@link TemplateEngine} is
 * a real instance with a {@link StringTemplateResolver} so we exercise the
 * actual Thymeleaf inline-expression substitution.
 */
@ExtendWith(MockitoExtension.class)
class TemplateRendererTest {

    @Mock private TemplateRepository templateRepository;

    private TemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        TemplateEngine engine = new TemplateEngine();
        engine.addTemplateResolver(new StringTemplateResolver());
        renderer = new TemplateRenderer(templateRepository, engine);
    }

    @Test
    void render_substitutesInlineExpressions() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(1L)
                .eventType("WELCOME")
                .subjectTemplate("Добро пожаловать, [[${name}]]!")
                .bodyTemplate("Здравствуйте, [[${name}]]! Ваш email: [[${email}]]")
                .build();
        when(templateRepository.findByEventType("WELCOME")).thenReturn(Optional.of(template));

        TemplateRenderer.Rendered result = renderer.render("WELCOME",
                Map.of("name", "Alice", "email", "alice@example.com"));

        assertThat(result.subject()).isEqualTo("Добро пожаловать, Alice!");
        assertThat(result.body()).isEqualTo("Здравствуйте, Alice! Ваш email: alice@example.com");
    }

    @Test
    void render_substitutesNumericValues() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(2L)
                .eventType("NEW_ORDER_SELLER")
                .subjectTemplate("Заказ #[[${orderId}]]")
                .bodyTemplate("Сумма: [[${amount}]]")
                .build();
        when(templateRepository.findByEventType("NEW_ORDER_SELLER")).thenReturn(Optional.of(template));

        TemplateRenderer.Rendered result = renderer.render("NEW_ORDER_SELLER",
                Map.of("orderId", 42L, "amount", 199.99));

        assertThat(result.subject()).isEqualTo("Заказ #42");
        assertThat(result.body()).isEqualTo("Сумма: 199.99");
    }

    @Test
    void render_throwsWhenTemplateMissing() {
        when(templateRepository.findByEventType("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renderer.render("UNKNOWN", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void render_handlesNullVariablesGracefully() {
        // Verify that a Context with no variables still works for a fragment
        // that has no inline expressions.
        NotificationTemplate template = NotificationTemplate.builder()
                .id(3L)
                .eventType("PLAIN")
                .subjectTemplate("Static subject")
                .bodyTemplate("Static body")
                .build();
        when(templateRepository.findByEventType("PLAIN")).thenReturn(Optional.of(template));

        TemplateRenderer.Rendered result = renderer.render("PLAIN", Map.of());
        assertThat(result.subject()).isEqualTo("Static subject");
        assertThat(result.body()).isEqualTo("Static body");
    }

    @Test
    void render_returnsRenderedRecord() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(4L)
                .eventType("X")
                .subjectTemplate("S")
                .bodyTemplate("B")
                .build();
        when(templateRepository.findByEventType("X")).thenReturn(Optional.of(template));

        TemplateRenderer.Rendered result = renderer.render("X", Map.of());

        assertThat(result).isNotNull();
        assertThat(result.subject()).isEqualTo("S");
        assertThat(result.body()).isEqualTo("B");
    }

    @Test
    void context_holdsVariables() {
        // Sanity-check the Thymeleaf Context API used by the renderer.
        Context ctx = new Context();
        ctx.setVariable("k", "v");
        assertThat(ctx.getVariable("k")).isEqualTo("v");
    }
}
