package io.linkinben.springbootsecurityjwt.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Entity
@Table(name = "users")
public class Users {

	@Id
	@Column
	private String uId;

	@NotBlank
	@Email
	@Column(length = 120, unique = true)
	private String email;

	@NotBlank
	@Column(name = "full_name", length = 100)
	private String fullName;

	@NotBlank
	@Column(length = 100)
	private String password;

	private String getuId() {
		return uId;
	}

	private void setuId(String uId) {
		this.uId = uId;
	}

	private String getEmail() {
		return email;
	}

	private void setEmail(String email) {
		this.email = email;
	}

	private String getFullName() {
		return fullName;
	}

	private void setFullName(String fullName) {
		this.fullName = fullName;
	}

	private String getPassword() {
		return password;
	}

	private void setPassword(String password) {
		this.password = password;
	}

	public Users() {
		
	}
	
	public Users(String uId, @NotBlank @Email String email, @NotBlank String fullName, @NotBlank String password) {
		this.uId = uId;
		this.email = email;
		this.fullName = fullName;
		this.password = password;
	}
}
