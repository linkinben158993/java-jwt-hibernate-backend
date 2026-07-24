package io.linkinben.springbootsecurityjwt.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.linkinben.springbootsecurityjwt.entities.Users;

/**
 * Dedicated request body for user registration / admin creation. Binds ONLY the client-settable
 * profile fields — never uId or roles (closes the mass-assignment gap G15) — and validates them
 * via @Valid so bad input yields a 400 instead of reaching persistence.
 */
@JsonIgnoreProperties(ignoreUnknown = true)   // tolerate extra fields (uId/roles) from entity-shaped payloads
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String fullName;

    @NotBlank
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /** Maps to a Users entity carrying only the three client fields (no id, no roles). */
    public Users toUser() {
        Users user = new Users();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(password);
        return user;
    }
}
