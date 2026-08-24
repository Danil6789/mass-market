package com.marketplace.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Thymeleaf {@link TemplateEngine} dedicated to processing inline template
 * <em>strings</em> (i.e. the contents of {@code notification_template.subject_template}
 * and {@code body_template}) as opposed to template names resolved from the
 * classpath.
 *
 * <p>This bean is consumed by {@link com.marketplace.notification.templates.TemplateRenderer}.
 * Spring Boot's auto-configured {@code SpringTemplateEngine} is left alone for
 * any future server-side HTML rendering of admin pages.</p>
 */
@Configuration
public class TemplateEngineConfig {

    @Bean
    public TemplateEngine stringTemplateEngine() {
        TemplateEngine engine = new TemplateEngine();
        engine.addTemplateResolver(new StringTemplateResolver());
        return engine;
    }
}
