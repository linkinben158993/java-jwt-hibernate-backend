package io.linkinben.springbootsecurityjwt.configs;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import io.linkinben.springbootsecurityjwt.tracing.TracingFilter;

/**
 * Registers {@link TracingFilter} at {@code HIGHEST_PRECEDENCE} — ahead of the Spring Security filter
 * chain (order -100) — so the trace id exists before any security or application code runs.
 *
 * <p>The filter is registered here (not annotated {@code @Component}) to avoid Boot's servlet-filter
 * auto-registration adding it a second time at the default order.
 */
@Configuration
public class TracingConfig {

    @Bean
    public FilterRegistrationBean<TracingFilter> tracingFilter() {
        FilterRegistrationBean<TracingFilter> registration = new FilterRegistrationBean<>(new TracingFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
