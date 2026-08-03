package io.linkinben.springbootsecurityjwt.tracing;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.JwtService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates the request-tracing feature end-to-end on ONE controller and ONE service: a single request
 * to {@code GET /api/users/me} passes through {@code UserAPIController#getCurrentUser} (traced at INFO)
 * and {@code UserServiceImpl#findByEmail} (traced at DEBUG), and both log lines carry the SAME trace id
 * — the one echoed back on the {@code X-Correlation-Id} response header.
 *
 * <p>{@code UserService} is intentionally the REAL bean so the aspect actually advises the service
 * layer; only the repositories are mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestTracingIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService; // real — mints a valid access token for the request

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;
    // UserService is intentionally the REAL bean so UserServiceImpl#findByEmail is traced by the aspect.

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private Logger aspectLogger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        aspectLogger = (Logger) LoggerFactory.getLogger(TracingAspect.class);
        originalLevel = aspectLogger.getLevel();
        aspectLogger.setLevel(Level.DEBUG); // capture the service-layer DEBUG entries too
        appender = new ListAppender<>();
        appender.start();
        aspectLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        aspectLogger.detachAppender(appender);
        aspectLogger.setLevel(originalLevel);
    }

    @Test
    void singleRequest_controllerAndServiceShareTheCorrelationId() throws Exception {
        String token = arrangeAuthenticatedUser();

        MvcResult result = mockMvc.perform(get("/api/users/me").header("access_token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andReturn();

        String responseCorrelationId = result.getResponse().getHeader("X-Correlation-Id");
        assertThat(responseCorrelationId).isNotBlank();

        ILoggingEvent controllerEntry = findEntry("UserAPIController#getCurrentUser");
        ILoggingEvent serviceEntry = findEntry("UserServiceImpl#findByEmail");

        // Both layers were traced (the controller at INFO, the service at DEBUG)...
        assertThat(controllerEntry).as("controller entry log").isNotNull();
        assertThat(serviceEntry).as("service entry log").isNotNull();
        assertThat(controllerEntry.getLevel()).isEqualTo(Level.INFO);
        assertThat(serviceEntry.getLevel()).isEqualTo(Level.DEBUG);

        // ...and both carry the same trace id — the one returned on the response header.
        String controllerCorrelationId = controllerEntry.getMDCPropertyMap().get("correlationId");
        String serviceCorrelationId = serviceEntry.getMDCPropertyMap().get("correlationId");
        assertThat(controllerCorrelationId).isEqualTo(responseCorrelationId);
        assertThat(serviceCorrelationId).isEqualTo(responseCorrelationId);
    }

    // Case 1 — the front-end SENDS X-Correlation-Id: the backend honours it (echoes it back unchanged and
    // traces under it), it does NOT regenerate one. This is the client-correlation path.
    @Test
    void inboundRequestId_fromClient_isHonouredNotRegenerated() throws Exception {
        String token = arrangeAuthenticatedUser();
        String clientCorrelationId = "client-correlation-2f8c1e40"; // a client-supplied id (not a UUID)

        mockMvc.perform(get("/api/users/me")
                        .header("access_token", "Bearer " + token)
                        .header("X-Correlation-Id", clientCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", clientCorrelationId)); // echoed back unchanged

        ILoggingEvent controllerEntry = findEntry("UserAPIController#getCurrentUser");
        assertThat(controllerEntry).as("controller entry log").isNotNull();
        assertThat(controllerEntry.getMDCPropertyMap().get("correlationId")).isEqualTo(clientCorrelationId);
    }

    // Case 2 — the front-end sends NOTHING: the backend generates a fresh UUID and traces under it.
    @Test
    void noInboundRequestId_backendGeneratesUuid() throws Exception {
        String token = arrangeAuthenticatedUser();

        MvcResult result = mockMvc.perform(get("/api/users/me").header("access_token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andReturn();

        String generated = result.getResponse().getHeader("X-Correlation-Id");
        assertThat(generated).isNotBlank();
        assertThat(UUID.fromString(generated)).isNotNull(); // a real UUID, not an echoed client string

        ILoggingEvent controllerEntry = findEntry("UserAPIController#getCurrentUser");
        assertThat(controllerEntry).as("controller entry log").isNotNull();
        assertThat(controllerEntry.getMDCPropertyMap().get("correlationId")).isEqualTo(generated);
    }

    // Arrange a user the filter can authenticate and the (real) service can resolve; return a valid token.
    private String arrangeAuthenticatedUser() {
        String email = "user@example.com";
        CustomUserDetails principal = new CustomUserDetails(
                "uid-1", "User", email, encoder.encode("pw"),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsServiceImpl.loadUserByUsername(email)).thenReturn(principal);

        Users me = new Users();
        me.setEmail(email);
        me.setFullName("User");
        Roles role = new Roles();
        role.setrName("ROLE_USER");
        me.setRoles(Set.of(role));
        when(userRepository.findByEmail(email)).thenReturn(me);

        return jwtService.genToken(principal);
    }

    private ILoggingEvent findEntry(String whereContains) {
        return appender.list.stream()
                .filter(e -> e.getFormattedMessage().contains(whereContains))
                .findFirst()
                .orElse(null);
    }
}
