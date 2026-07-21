package io.linkinben.springbootsecurityjwt.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.linkinben.springbootsecurityjwt.dtos.AuthenticationRequest;
import io.linkinben.springbootsecurityjwt.dtos.AuthenticationResponse;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.services.UserService;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;

@Slf4j
@RestController
@RequestMapping("api/auth")
public class AuthenticationController {

	// TODO: Move to application config or DB table — hardcoded whitelist is dev-only.
	// Loop back to make this dynamic (e.g. spring.security.oauth2.whitelist.admin / .user
	// or a DB-backed allowed_oauth2_emails table with a role column).
	// Current whitelisted accounts:
	//   ADMIN — thienan.nguyenhoang311@gmail.com
	//		   — thienan.nguyenhoang011@gmail.com
	//   USER  — thienan.nguyenhoang.411@gmail.com
	// Only emails present in either list are permitted; all others are rejected with 403.
	private static final Set<String> OAUTH2_ADMIN_EMAILS = Set.of(
			"thienan.nguyenhoang311@gmail.com", "thienan.nguyenhoang011@gmail.com"
	);
	private static final Set<String> OAUTH2_USER_EMAILS = Set.of(
			"thienan.nguyenhoang.411@gmail.com"
	);

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JWTUtils jwtUtils;

	@Autowired
	private UserService userService;

	@Autowired
	private UserDetailsServiceImpl userDetailsServiceImpl;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TokenBlacklistService tokenBlacklistService;

	@Value("${okta.oauth2.clientId}")
	private String clientId;

	@Value("${okta.oauth2.clientSecret}")
	private String clientSecret;

	@Value("${auth0.logout.domain}")
	private String auth0LogoutDomain;

	@Value("${auth0.logout.client-id}")
	private String auth0LogoutClientId;

	@Value("${auth0.logout.return-to}")
	private String auth0LogoutReturnTo;

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

			String role = customUserDetails.getAuthorities().stream()
					.map(a -> a.getAuthority()).findFirst().orElse("ROLE_USER");
			userInfo.put("accessToken", jwt);
			userInfo.put("refreshToken", jwt_refresh);
			userInfo.put("uName", customUserDetails.getUsername());
			userInfo.put("uId", customUserDetails.getuId());
			userInfo.put("role", role);

			response.put("title", "Good Credential!");
			response.put("message", "Access Granted!");
			response.put("data", userInfo);
			AuthenticationResponse authenticationResponse = new AuthenticationResponse(response);
			return new ResponseEntity<Object>(authenticationResponse, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Login authentication failed", e);
			response.put("title", "Bad Credential!");
			response.put("message", "Access Denied!");
			return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/oauth2/login", method = RequestMethod.POST)
	public ResponseEntity<?> loginWithOAuth2Credential(@RequestBody Map<String, String> body) {
		Map<String, Object> response = new HashMap<>();
		try {
			String credential = body.get("credential");
			String subJson = jwtUtils.extractCredentialSubject(credential);

			Map<String, Object> credentialData = objectMapper.readValue(subJson, new TypeReference<>() {});
			String email = (String) credentialData.get("email");

			String fullName = email;
			Object infoObj = credentialData.get("info");
			if (infoObj instanceof Map<?, ?> info && info.get("name") instanceof String name) {
				fullName = name;
			}

			boolean isAdmin = OAUTH2_ADMIN_EMAILS.contains(email);
			boolean isUser  = OAUTH2_USER_EMAILS.contains(email);
			if (!isAdmin && !isUser) {
				log.warn("OAuth2 login rejected — email not in whitelist: {}", email);
				response.put("title", "Access Denied!");
				response.put("message", "This Google account is not authorised.");
				return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
			}

			Users existing = userService.findByEmail(email);
			if (existing == null) {
				String roleName = isAdmin ? "ROLE_ADMIN" : "ROLE_USER";
				Users newUser = new Users();
				newUser.setEmail(email);
				newUser.setFullName(fullName);
				newUser.setPassword(UUID.randomUUID().toString());
				userService.add(newUser, roleName);
				log.info("Created OAuth2 user: {} with role: {}", email, roleName);
			} else {
				log.debug("OAuth2 user already exists: {}", email);
			}

			CustomUserDetails userDetails = (CustomUserDetails) userDetailsServiceImpl.loadUserByUsername(email);
			String accessToken = jwtUtils.genToken(userDetails, "oauth2");
			String refreshToken = jwtUtils.genRefreshToken(userDetails);

			String role = userDetails.getAuthorities().stream()
					.map(a -> a.getAuthority()).findFirst().orElse("ROLE_USER");
			Map<String, Object> userInfo = new HashMap<>();
			userInfo.put("accessToken", accessToken);
			userInfo.put("refreshToken", refreshToken);
			userInfo.put("uName", userDetails.getUsername());
			userInfo.put("uId", userDetails.getuId());
			userInfo.put("role", role);

			response.put("title", "Good Credential!");
			response.put("message", "Access Granted!");
			response.put("data", userInfo);
			return new ResponseEntity<>(new AuthenticationResponse(response), HttpStatus.OK);

		} catch (Exception e) {
			log.error("OAuth2 credential exchange failed", e);
			response.put("title", "Bad Credential!");
			response.put("message", "Access Denied!");
			return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
		}
	}

	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public ResponseEntity<?> logout(HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
		String header = request.getHeader("access_token");
		if (header != null && header.startsWith("Bearer ")) {
			String rawJwt = header.substring(7);
			try {
				long expiresAtMs = jwtUtils.extractExpiration(header).getTime();
				tokenBlacklistService.add(rawJwt, expiresAtMs);
				String loginMethod = jwtUtils.extractLoginMethod(header);
				if ("oauth2".equals(loginMethod)) {
					String returnTo = URLEncoder.encode(auth0LogoutReturnTo, StandardCharsets.UTF_8);
					String auth0LogoutUrl = String.format(
							"https://%s/v2/logout?returnTo=%s&client_id=%s",
							auth0LogoutDomain, returnTo, auth0LogoutClientId
					);
					response.put("auth0LogoutUrl", auth0LogoutUrl);
					log.info("OAuth2 logout for token — Auth0 session termination URL returned");
				} else {
					log.info("Password logout — token blacklisted");
				}
			} catch (Exception e) {
				log.warn("Logout called with unreadable token: {}", e.getMessage());
			}
		}
		return ResponseEntity.ok(response);
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
			log.debug("OIDC user principal: {}", user);
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

	@RequestMapping(value = "/token/refresh", method = RequestMethod.GET)
	public ResponseEntity<?> refreshToken(@RequestBody AuthenticationRequest authenticationRequest) {
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("title", "Good Credential!");
		response.put("message", "Access Granted!");
		response.put("data", "Motherfucker!");
		AuthenticationResponse authenticationResponse = new AuthenticationResponse(response);
		return new ResponseEntity<Object>(authenticationResponse, HttpStatus.OK);
	}
}
