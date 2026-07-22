package io.linkinben.springbootsecurityjwt.configs;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Replaces the real SecurityFilterChain for @WebMvcTest controller unit tests.
 *
 * Problem: MvcRequestMatcher (Spring Security 6 default) relies on HandlerMappingIntrospector
 * which needs all MVC handlers to be registered. In @WebMvcTest only one controller is loaded,
 * so path-based security rules never match — every request falls to anyRequest().authenticated(),
 * causing 302 redirects or 403 errors unrelated to controller logic.
 *
 * Solution: Register this chain at HIGHEST_PRECEDENCE so it intercepts all requests before the
 * real SecurityConfig chain. Controller unit tests validate controller logic in isolation; security
 * enforcement is covered by SecurityFilterChainIT (a @SpringBootTest integration test).
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
