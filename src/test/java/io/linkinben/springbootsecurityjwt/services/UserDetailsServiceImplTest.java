package io.linkinben.springbootsecurityjwt.services;

import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserDetailsServiceImpl userDetailsServiceImpl;

    private Users user;
    private Roles role;

    @BeforeEach
    void setUp() {
        role = new Roles("role-id", "ROLE_USER", null);
        user = new Users("uid-123", "test@example.com", "Test User", "hashed-pw");
        user.setRoles(Set.of(role));
    }

    // --- 5.1 loadUserByUsername found — returns CustomUserDetails with correct fields ---
    @Test
    void loadUserByUsername_found_returnsCustomUserDetails() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(user);

        UserDetails result = userDetailsServiceImpl.loadUserByUsername("test@example.com");

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails details = (CustomUserDetails) result;
        assertThat(details.getUsername()).isEqualTo("test@example.com");
        assertThat(details.getuId()).isEqualTo("uid-123");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    // --- 5.2 loadUserByUsername not found — throws BadCredentialsException ---
    @Test
    void loadUserByUsername_notFound_throwsBadCredentialsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(null);

        assertThatThrownBy(() -> userDetailsServiceImpl.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(BadCredentialsException.class);
    }

    // --- 5.3 loadUserByUserId found — returns CustomUserDetails with correct username ---
    @Test
    void loadUserByUserId_found_returnsCustomUserDetails() {
        when(userRepository.findById("uid-123")).thenReturn(user);

        UserDetails result = userDetailsServiceImpl.loadUserByUserId("uid-123");

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("test@example.com");
    }
}
