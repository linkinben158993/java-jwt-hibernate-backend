package io.linkinben.springbootsecurityjwt.dtos;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails extends User implements UserDetails {

	private static final long serialVersionUID = 1L;

	private String uId;

	private String fullName;

	public String getuId() {
		return uId;
	}

	public void setuId(String uId) {
		this.uId = uId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public CustomUserDetails(String uId, String email, String password,
			Collection<? extends GrantedAuthority> authorities) {
		super(email, password, authorities);
		this.uId = uId;
		this.uFullName = uName;
	}
	
	// TODO: Set more custom details lessen database querying below


}
