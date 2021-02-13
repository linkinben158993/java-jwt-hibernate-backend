package io.linkinben.springbootsecurityjwt.dto;

import java.util.ArrayList;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return new User("an","$2a$10$gPmwbAZ4jEK8GAWwImu8Ku1ybZF7.rp829r40iJH.YsBA4kdIJLgK", new ArrayList<GrantedAuthority>());
	}

}
