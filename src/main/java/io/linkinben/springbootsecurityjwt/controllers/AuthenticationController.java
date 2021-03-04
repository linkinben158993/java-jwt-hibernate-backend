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
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					authenticationRequest.getUsername(), authenticationRequest.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);

			CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

			final String jwt = jwtUtils.genToken(customUserDetails);
			Map<String, Object> userInfo = new HashMap<String, Object>();
			userInfo.put("uId", customUserDetails.getuId());
			userInfo.put("uName", customUserDetails.getUsername());
			userInfo.put("accessToken", jwt);
			
			response.put("title", "Good Credential!");
			response.put("message", "Access Granted!");
			response.put("data", userInfo);
			AuthenticationResponse authenticationResponse = new AuthenticationResponse(response);
			return new ResponseEntity<Object>(authenticationResponse, HttpStatus.OK);

		} catch (Exception e) {
			System.out.println("Not Okay");
			System.out.println(e);
			response.put("title", "Bad Credential!");
			response.put("message", "Access Denied!");
			return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
		}
	}
}
