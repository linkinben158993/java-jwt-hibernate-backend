package io.linkinben.springbootsecurityjwt.tracing;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns each request a correlation id (reusing an inbound {@code X-Correlation-Id} if the front-end
 * sent one, else a fresh UUID), puts it in MDC so every log line for the request carries it, echoes it
 * back on the response, and clears MDC in a {@code finally} so ids never leak across pooled Tomcat
 * threads.
 *
 * <p>Registered at {@code HIGHEST_PRECEDENCE} (see {@code TracingConfig}) so it runs before the Spring
 * Security chain — security logs then already carry the id.
 */
public class TracingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = Optional.ofNullable(request.getHeader(MdcKeys.CORRELATION_ID_HEADER))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());
        MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        response.setHeader(MdcKeys.CORRELATION_ID_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
