package io.linkinben.springbootsecurityjwt.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.linkinben.springbootsecurityjwt.dtos.AuthenticationRequest;
import io.linkinben.springbootsecurityjwt.dtos.AuthenticationResponse;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;

@Controller
@RequestMapping("authenticate")
public class AuthenticationController {
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JWTUtils jwtUtils;

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ResponseEntity<?> createAuthJWT(@RequestBody AuthenticationRequest authenticationRequest) {
		try {
			System.out.println(authenticationRequest.getUsername());
			System.out.println(authenticationRequest.getPassword());
			Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					authenticationRequest.getUsername(), authenticationRequest.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);

			CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
			System.out.println(customUserDetails.getuId());
			System.out.println(customUserDetails.getUsername());

			final String jwt = jwtUtils.genToken(customUserDetails);

			Map<String, Object> response = new HashMap<String, Object>();
			response.put("title", "Good Credential!");
			response.put("message", "Access Granted!");
			response.put("data", jwt);
			AuthenticationResponse authenticationResponse = new AuthenticationResponse(response);
			System.out.println("Okay Motherfucker!");
			return new ResponseEntity<Object>(authenticationResponse, HttpStatus.OK);

		} catch (Exception e) {
			System.out.println("Not Okay");
			System.out.println(e);
			return new ResponseEntity<Object>("User Credential Incorrect!", HttpStatus.BAD_REQUEST);
		}
	}
}
