package io.linkinben.springbootsecurityjwt.controllers;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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

	@Value("${okta.oauth2.clientId}")
	private String clientId;
	
	@Value("${okta.oauth2.clientSecret}")
	private String clientSecret;
	
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ResponseEntity<?> createAuthJWT(@RequestBody AuthenticationRequest authenticationRequest) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					authenticationRequest.getUsername(), authenticationRequest.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);

			CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

			final String jwt = jwtUtils.genToken(customUserDetails);
			final String jwt_refresh = jwtUtils.genRefreshToken(customUserDetails);
			Map<String, Object> userInfo = new HashMap<String, Object>();

			// Unnecessary if cast refresh token
			// userInfo.put("uId", customUserDetails.getuId());
			// userInfo.put("uName", customUserDetails.getUsername());
			userInfo.put("accessToken", jwt);
			userInfo.put("refreshToken", jwt_refresh);

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

	@RequestMapping(value = "/okta", method = RequestMethod.GET)
	public ResponseEntity<?> getOktaInfo(@RequestParam(required = false) String code,
			@RequestParam(required = false) String state, @AuthenticationPrincipal OidcUser user) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("client-id", this.clientId);
		data.put("client-secret", this.clientSecret);
		data.put("code", "default");
		data.put("state", "default");
		try {
			System.out.println("Some name: " + user);
			data.put("code", code);
			data.put("state", state);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("title", "Good Credential!");
		response.put("message", "Access Granted!");
		response.put("data", data);
		AuthenticationResponse authenticationResponse = new AuthenticationResponse(response);
		return new ResponseEntity<Object>(authenticationResponse, HttpStatus.OK);
	}

	@RequestMapping(value = "/refresh_token", method = RequestMethod.GET)
	public ResponseEntity<?> refreshToken(@RequestBody AuthenticationRequest authenticationRequest) {
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("title", "Good Credential!");
		response.put("message", "Access Granted!");
		response.put("data", "Motherfucker!");
		AuthenticationResponse authenticationResponse = new AuthenticationResponse(response);
		return new ResponseEntity<Object>(authenticationResponse, HttpStatus.OK);
	}
}
