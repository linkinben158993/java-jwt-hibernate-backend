package io.linkinben.springbootsecurityjwt.configs;

import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthenticationHandler customAuthHandler;

    @Autowired
    private UserDetailsServiceImpl myUserDetailService;

    @Autowired
    private RequestFilterConfig requestFilterConfig;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(myUserDetailService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/home/**", "/api/auth/**", "/oauth/**",
                    "/error/**",
                    "/ws/**", "/topic", "/app/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers(
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/roles").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/without-role").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users/admin").hasRole("ADMIN")
                .requestMatchers("/api/roles").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                // Only REST API paths get a 401 JSON error — browser navigations (e.g. OAuth2 flow)
                // still receive the default redirect-to-login behaviour from oauth2Login().
                .defaultAuthenticationEntryPointFor(
                    (request, response, e) ->
                        response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"),
                    // getRequestURI() is set correctly in both MockMvc and real Tomcat (at root context path).
                    // getServletPath() returns "" in MockMvc, breaking integration test 401 assertions.
                    request -> request.getRequestURI().startsWith("/api/")
                )
            )
            /*
             * OAuth2 login — Spring Security internals (NOT unit/slice testable).
             *
             * What happens here at runtime:
             *   1. Browser hits GET /oauth2/authorization/auth0
             *      → OAuth2AuthorizationRequestRedirectFilter (framework) builds the Auth0
             *        authorization URL and sends the 302. No application code runs.
             *        Provider: Auth0 tenant dev-cubyp8uiwwf8ldh3.au.auth0.com
             *        Social connection: google-oauth2 (Google as identity provider via Auth0)
             *
             *   2. User authenticates at Google / Auth0 Universal Login.
             *      Auth0 posts the authorization code back to our registered redirect URI.
             *      → OAuth2LoginAuthenticationFilter (framework) exchanges the code for tokens
             *        and constructs the OidcUser principal. No application code runs.
             *
             *   3. Spring Security calls our successHandler / failureHandler.
             *      → These ARE unit-tested in AuthenticationHandlerTest.
             *        The full callback chain (steps 1–2) requires a live Auth0 tenant and a
             *        real browser session — verified manually (verification-checklist.md §5.1–5.3).
             */
            .oauth2Login(oauth -> oauth
                .successHandler(customAuthHandler.successHandler)
                .failureHandler(customAuthHandler.failureHandler)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(requestFilterConfig, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
