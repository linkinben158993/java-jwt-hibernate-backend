package io.linkinben.springbootsecurityjwt.dtos;

import jakarta.validation.constraints.NotBlank;

public class ChangePasswordDTO {

	// Email is derived from the authenticated principal, not the body — no constraint.
	private String email;

	@NotBlank
	private String password;

	public ChangePasswordDTO(String email, String password) {
		this.email = email;
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
