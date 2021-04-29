package io.linkinben.springbootsecurityjwt.dtos;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails extends User implements UserDetails {

	private static final long serialVersionUID = 1L;

	private String uId;

	public String getuId() {
		return uId;
	}

	public void setuId(String uId) {
		this.uId = uId;
	}

	private String uFullName;

	public String getuFullName() {
		return uFullName;
	}

	public void setuFullName(String uFullName) {
		this.uFullName = uFullName;
	}

	public CustomUserDetails(String uId, String uName, String email, String password,
			Collection<? extends GrantedAuthority> authorities) {
		super(email, password, authorities);
		this.uId = uId;
		this.uFullName = uName;
	}
}
