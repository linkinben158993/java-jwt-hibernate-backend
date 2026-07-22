package io.linkinben.springbootsecurityjwt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.RoleService;
import io.linkinben.springbootsecurityjwt.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the real SecurityFilterChain rules using a full Spring context.
 *
 * @SpringBootTest loads all beans (including SecurityConfig with its MvcRequestMatcher rules),
 * which is why these tests work correctly. @WebMvcTest slices cannot test security enforcement
 * because MvcRequestMatcher requires all MVC handlers to be registered.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFilterChainIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Prevent PersistenceAnnotationBeanPostProcessor from failing — same pattern as
    // SpringbootSecurityJwtApplicationTests. Repos carry @PersistenceContext EntityManager
    // which requires a DataSource excluded in application-test.yml.
    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;

    // Mock the service layer so controller logic returns clean 200s for admission tests.
    @MockitoBean private UserService userService;
    @MockitoBean private RoleService roleService;

    @BeforeEach
    void setUp() {
        when(userService.findAll()).thenReturn(List.of());
    }

    // --- Public paths (permitAll) ---

    @Test
    @WithAnonymousUser
    void publicPath_anonymous_returns200() throws Exception {
        mockMvc.perform(get("/home/hello-world"))
                .andExpect(status().isOk());
    }

    // --- GET /api/users — ADMIN-only ---

    @Test
    @WithAnonymousUser
    void getUsers_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUsers_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    // --- POST /api/users/admin — ADMIN-only (privilege escalation gate) ---

    @Test
    @WithMockUser(roles = "USER")
    void createAdmin_asUser_returns403() throws Exception {
        mockMvc.perform(post("/api/users/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/users/roles — ADMIN-only ---

    @Test
    @WithMockUser(roles = "USER")
    void getUsersWithRole_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/users/roles"))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/users/without-role — ADMIN-only ---

    @Test
    @WithMockUser(roles = "USER")
    void getUsersWithoutRole_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/users/without-role"))
                .andExpect(status().isForbidden());
    }

    // --- POST /api/roles — ADMIN-only ---

    @Test
    @WithAnonymousUser
    void createRole_anonymous_returns401() throws Exception {
        Roles role = new Roles();
        role.setrName("ROLE_MODERATOR");
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createRole_asUser_returns403() throws Exception {
        Roles role = new Roles();
        role.setrName("ROLE_MODERATOR");
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isForbidden());
    }
}
