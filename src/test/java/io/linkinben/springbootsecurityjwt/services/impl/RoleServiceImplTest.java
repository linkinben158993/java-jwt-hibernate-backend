package io.linkinben.springbootsecurityjwt.services.impl;

import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.repositories.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock private RoleRepository roleRepository;
    @InjectMocks private RoleServiceImpl roleServiceImpl;

    private Roles role;

    @BeforeEach
    void setUp() {
        role = new Roles();
        role.setrName("ROLE_USER");
    }

    // --- 7.1 add generates UUID and inserts role ---
    @Test
    void add_generatesUuidAndInsertsRole() {
        roleServiceImpl.add(role);

        ArgumentCaptor<Roles> captor = ArgumentCaptor.forClass(Roles.class);
        verify(roleRepository).insert(captor.capture());
        assertThat(captor.getValue().getrId()).isNotBlank();
        assertThat(captor.getValue().getrName()).isEqualTo("ROLE_USER");
    }

    // --- 7.2 findByRoleName delegates to repository ---
    @Test
    void findByRoleName_delegatesToRepository() {
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(role);
        assertThat(roleServiceImpl.findByRoleName("ROLE_USER")).isEqualTo(role);
    }

    // --- 7.3 edit completes without error (no-op in RoleRepositoryImpl) ---
    @Test
    void edit_completesWithoutError() {
        roleServiceImpl.edit(role);
        verify(roleRepository).update(role);
    }
}
