package io.linkinben.springbootsecurityjwt.security;

import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.RoleService;
import io.linkinben.springbootsecurityjwt.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Enforcement of the ownership/rank rule through the real filter chain + method security (plan §4.2).
 * Uses the {@code authentication(...)} post-processor to supply a real {@link CustomUserDetails}
 * principal (the policy bean reads {@code getuId()} — {@code @WithMockUser} would give a plain User).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserResourceAuthorizationIT {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserService userService;
    @MockitoBean private RoleService roleService;

    // --- PATCH /api/users/{id} : owner or higher rank ---

    @Test
    void ownerEditsSelf_returns200() throws Exception {
        when(userService.findById("me")).thenReturn(userWith("me", "ROLE_USER"));
        mockMvc.perform(patch("/api/users/me").with(asUser("me", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"New\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminEditsRegularUser_returns200() throws Exception {
        when(userService.findById("bob")).thenReturn(userWith("bob", "ROLE_USER"));
        mockMvc.perform(patch("/api/users/bob").with(asUser("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"New\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminEditsPeerAdmin_returns404() throws Exception {
        when(userService.findById("carol")).thenReturn(userWith("carol", "ROLE_ADMIN"));
        mockMvc.perform(patch("/api/users/carol").with(asUser("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"New\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void userEditsAnotherUser_returns404() throws Exception {
        when(userService.findById("dave")).thenReturn(userWith("dave", "ROLE_USER"));
        mockMvc.perform(patch("/api/users/dave").with(asUser("bob", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"New\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousEdit_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/bob").with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"New\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- PATCH /api/users/{id}/role : granted role must rank below the caller ---

    @Test
    void adminGrantsUserRole_returns200() throws Exception {
        when(userService.findById("bob")).thenReturn(userWith("bob", "ROLE_USER"));
        mockMvc.perform(patch("/api/users/bob/role").with(asUser("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROLE_USER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminGrantsAdminRole_returns403() throws Exception {
        when(userService.findById("bob")).thenReturn(userWith("bob", "ROLE_USER"));
        mockMvc.perform(patch("/api/users/bob/role").with(asUser("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userGrantsRole_returns403() throws Exception {
        mockMvc.perform(patch("/api/users/bob/role").with(asUser("u", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROLE_USER\"}"))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /api/users/{id} : owner or higher rank ---

    @Test
    void adminDeletesRegularUser_returns200() throws Exception {
        when(userService.findById("bob")).thenReturn(userWith("bob", "ROLE_USER"));
        when(userService.delete("bob")).thenReturn(1);
        mockMvc.perform(delete("/api/users/bob").with(asUser("admin", "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void userDeletesAnotherUser_returns404() throws Exception {
        when(userService.findById("dave")).thenReturn(userWith("dave", "ROLE_USER"));
        mockMvc.perform(delete("/api/users/dave").with(asUser("bob", "ROLE_USER")))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private RequestPostProcessor asUser(String uId, String role) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        CustomUserDetails principal =
                new CustomUserDetails(uId, "name-" + uId, uId + "@example.com", "pw", authorities);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private Users userWith(String uId, String... roleNames) {
        Users u = new Users();
        u.setuId(uId);
        Set<Roles> roles = new HashSet<>();
        for (String rn : roleNames) {
            Roles r = new Roles();
            r.setrName(rn);
            roles.add(r);
        }
        u.setRoles(roles);
        return u;
    }
}
