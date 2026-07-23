package io.linkinben.springbootsecurityjwt.services.impl;

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.UserRepository;
import io.linkinben.springbootsecurityjwt.services.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleService roleService;
    @InjectMocks private UserServiceImpl userServiceImpl;

    private Roles roleUser;
    private Roles roleAdmin;
    private Users user;

    @BeforeEach
    void setUp() {
        roleUser  = new Roles("role-user-id",  "ROLE_USER",  null);
        roleAdmin = new Roles("role-admin-id", "ROLE_ADMIN", null);
        user = new Users();
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPassword("plain-password");
    }

    // --- 6.1 add with ROLE_USER hashes password and inserts ---
    @Test
    void add_roleUser_hashesPasswordAndInsertsUser() {
        when(roleService.findByRoleName("ROLE_USER")).thenReturn(roleUser);

        userServiceImpl.add(user, "ROLE_USER");

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userRepository).insert(captor.capture());
        Users inserted = captor.getValue();

        assertThat(inserted.getuId()).isNotBlank();
        assertThat(BCrypt.checkpw("plain-password", inserted.getPassword())).isTrue();
        assertThat(inserted.getRoles()).extracting("rName").containsExactly("ROLE_USER");
    }

    // --- 6.2 add with ROLE_ADMIN attaches admin role ---
    @Test
    void add_roleAdmin_attachesAdminRole() {
        when(roleService.findByRoleName("ROLE_ADMIN")).thenReturn(roleAdmin);

        userServiceImpl.add(user, "ROLE_ADMIN");

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userRepository).insert(captor.capture());
        assertThat(captor.getValue().getRoles()).extracting("rName").containsExactly("ROLE_ADMIN");
    }

    // --- 6.3 findByEmail delegates to repository ---
    @Test
    void findByEmail_delegatesToRepository() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(user);
        assertThat(userServiceImpl.findByEmail("test@example.com")).isEqualTo(user);
    }

    // --- 6.4 editPassword hashes the new password before persisting ---
    @Test
    void editPassword_hashesNewPasswordBeforePersisting() {
        ChangePasswordDTO dto = new ChangePasswordDTO("test@example.com", "new-plain-password");
        when(userRepository.updatePassword(any())).thenReturn(1);

        userServiceImpl.editPassword(dto);

        ArgumentCaptor<ChangePasswordDTO> captor = ArgumentCaptor.forClass(ChangePasswordDTO.class);
        verify(userRepository).updatePassword(captor.capture());
        assertThat(BCrypt.checkpw("new-plain-password", captor.getValue().getPassword())).isTrue();
    }

    // --- 6.5 editUsersRole fetches ROLE_USER and calls batchUpdateUserRoleHQL ---
    @Test
    void editUsersRole_fetchesRoleUserAndCallsBatchHQL() {
        when(roleService.findByRoleName("ROLE_USER")).thenReturn(roleUser);

        userServiceImpl.editUsersRole();

        verify(userRepository).batchUpdateUserRoleHQL(argThat(roles ->
                roles.stream().anyMatch(r -> "ROLE_USER".equals(r.getrName()))
        ));
    }

    // --- 6.6 findAll delegates to repository ---
    @Test
    void findAll_delegatesToRepository() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        assertThat(userServiceImpl.findAll()).containsExactly(user);
    }
}
