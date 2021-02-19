package io.linkinben.springbootsecurityjwt.services;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Users user = userRepository.findByEmail(email);
		if (user == null)
			throw new BadCredentialsException("Given Credential Not Found");

		Set<GrantedAuthority> grantedAuthorities = new HashSet<GrantedAuthority>();

		for (Roles item : user.getRoles()) {
			grantedAuthorities.add(new SimpleGrantedAuthority(item.getrName()));
		}

		return new CustomUserDetails(user.getuId(), user.getEmail(), user.getPassword(), grantedAuthorities);
	}
}
