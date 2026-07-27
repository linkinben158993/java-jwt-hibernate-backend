package io.linkinben.springbootsecurityjwt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Manual plan D3, automated — logout blacklists the access token, and the same token is then rejected
 * on a protected endpoint. Uses the REAL {@code TokenBlacklistService} (in-memory) end-to-end.
 *
 * NOTE (distributed deployment): the blacklist is an in-memory {@code Map} — it is INSTANCE-LOCAL. This
 * test only proves the single-instance guarantee. Across a horizontally-scaled / multi-instance
 * deployment a token blacklisted on one node would still be accepted by others; that needs a shared
 * store (Redis / DB table). Tracked as the G11 persistence TODO.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoutBlacklistIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;
    @MockitoBean private UserService userService;
    // TokenBlacklistService is intentionally the REAL bean — it is the subject under test.

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void logout_blacklistsToken_thenProtectedCallReturns401() throws Exception {
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
        when(userService.findByEmail(email)).thenReturn(me);

        // 1. Login → access token.
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + email + "\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).path("response").path("data").path("accessToken").asText();

        // 2. Token is accepted before logout.
        mockMvc.perform(get("/api/users/me").header("access_token", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Logout blacklists the token.
        mockMvc.perform(post("/api/auth/logout").header("access_token", "Bearer " + token))
                .andExpect(status().isOk());

        // 4. The same token is now rejected on a protected endpoint.
        mockMvc.perform(get("/api/users/me").header("access_token", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
