package io.linkinben.springbootsecurityjwt.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.linkinben.springbootsecurityjwt.dto.AuthenticationRequest;
import io.linkinben.springbootsecurityjwt.dto.AuthenticationResponse;
import io.linkinben.springbootsecurityjwt.dto.CustomUserDetailsService;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;

@Controller
@RequestMapping("authenticate")
public class AuthenticationController {
	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private JWTUtils jwtUtils;
	
	@Autowired
	private CustomUserDetailsService customUserDetailsService;
	
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ResponseEntity<?> createAuthJWT(@RequestBody AuthenticationRequest authenticationRequest) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
			);
			System.out.println(authenticationRequest.getUsername());
			System.out.println(authenticationRequest.getPassword());
		}
		catch (BadCredentialsException e) {
			System.out.println("Not Okay");
			return new ResponseEntity<Object>("User Credential Incorrect!", HttpStatus.BAD_REQUEST);	
		}


		final UserDetails userDetails = customUserDetailsService
				.loadUserByUsername(authenticationRequest.getUsername());

		final String jwt = jwtUtils.genToken(userDetails);

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("title", "Good Credential!");
		response.put("message", "Access Granted!");		
		response.put("data", jwt);
		AuthenticationResponse authenticationResponse = new AuthenticationResponse(response);
		System.out.println("Okay Motherfucker!");
		return new ResponseEntity<Object>(authenticationResponse, HttpStatus.OK);	
	}
}
