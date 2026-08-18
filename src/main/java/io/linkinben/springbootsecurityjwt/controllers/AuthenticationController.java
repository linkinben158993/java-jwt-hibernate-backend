package io.linkinben.springbootsecurityjwt.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import io.linkinben.springbootsecurityjwt.api.AuthenticationApi;
import io.linkinben.springbootsecurityjwt.api.model.AuthenticationRequest;
import io.linkinben.springbootsecurityjwt.api.model.LoginResponse;
import io.linkinben.springbootsecurityjwt.api.model.LogoutResponse;
import io.linkinben.springbootsecurityjwt.api.model.OAuth2LoginRequest;
import io.linkinben.springbootsecurityjwt.api.model.OktaInfoResponse;
import io.linkinben.springbootsecurityjwt.api.model.RefreshResponse;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.events.UserLoggedOutEvent;
import io.linkinben.springbootsecurityjwt.exceptions.BadRequestException;
import io.linkinben.springbootsecurityjwt.exceptions.ForbiddenOperationException;
import io.linkinben.springbootsecurityjwt.exceptions.UnauthorizedException;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.services.UserService;
import io.linkinben.springbootsecurityjwt.services.JwtService;

/**
 * Contract-first authentication endpoints — implements the generated {@link AuthenticationApi}
 * interface (mappings come from the interface). Responses are flat, typed DTOs (O-4b); the access /
 * refresh token is carried in the standard {@code Authorization: Bearer <jwt>} header (O-5b).
 */
@Slf4j
@RestController
public class AuthenticationController implements AuthenticationApi {

	// TODO: Move to application config or DB table — hardcoded whitelist is dev-only.
	//   ADMIN — thienan.nguyenhoang311@gmail.com / thienan.nguyenhoang011@gmail.com
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
	private JwtService jwtService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserDetailsServiceImpl userDetailsServiceImpl;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TokenBlacklistService tokenBlacklistService;

	@Autowired
	private ApplicationEventPublisher publisher;

	// Request-scoped proxy — the interface signatures carry no HttpServletRequest, so read the
	// Authorization header (O-5b) from the current request here.
	@Autowired
	private HttpServletRequest request;

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

	@Override
	public ResponseEntity<LoginResponse> login(AuthenticationRequest authenticationRequest, String xCorrelationId) {
		// AuthenticationException (e.g. BadCredentialsException) propagates to GlobalExceptionHandler → 400.
		Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
				authenticationRequest.getUsername(), authenticationRequest.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authentication);

		CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

		final String jwt = jwtService.genToken(customUserDetails);
		final String jwt_refresh = jwtService.genRefreshToken(customUserDetails);

		String role = customUserDetails.getAuthorities().stream()
				.map(a -> a.getAuthority()).findFirst().orElse("ROLE_USER");

		LoginResponse response = new LoginResponse(
				jwt, jwt_refresh, customUserDetails.getuId(), customUserDetails.getUsername(), role);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<LoginResponse> loginWithOAuth2(OAuth2LoginRequest oauth2LoginRequest, String xCorrelationId) {
		String subJson = jwtService.extractCredentialSubject(oauth2LoginRequest.getCredential());

		Map<String, Object> credentialData;
		try {
			credentialData = objectMapper.readValue(subJson, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			throw new BadRequestException("Malformed credential");
		}
		String email = (String) credentialData.get("email");

		String fullName = email;
		Object infoObj = credentialData.get("info");
		if (infoObj instanceof Map<?, ?> info && info.get("name") instanceof String name) {
			fullName = name;
		}

		boolean isAdmin = OAUTH2_ADMIN_EMAILS.contains(email);
		boolean isUser  = OAUTH2_USER_EMAILS.contains(email);
		if (!isAdmin && !isUser) {
			log.warn("OAuth2 login rejected - email not in whitelist: {}", email);
			throw new ForbiddenOperationException("This Google account is not authorised.");
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
		String accessToken = jwtService.genToken(userDetails, "oauth2");
		String refreshToken = jwtService.genRefreshToken(userDetails, "oauth2");

		String role = userDetails.getAuthorities().stream()
				.map(a -> a.getAuthority()).findFirst().orElse("ROLE_USER");

		LoginResponse response = new LoginResponse(
				accessToken, refreshToken, userDetails.getuId(), userDetails.getUsername(), role);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<LogoutResponse> logout(String xCorrelationId) {
		LogoutResponse response = new LogoutResponse();
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			String rawJwt = header.substring(7);
			try {
				long expiresAtMs = jwtService.extractExpiration(header).getTime();
				String loginMethod = jwtService.extractLoginMethod(header);
				// Observer: publish once; the sync blacklist listener revokes the token, the async
				// audit listener records it (side effects moved out of the controller — P2 relocation).
				publisher.publishEvent(new UserLoggedOutEvent(rawJwt, expiresAtMs, loginMethod));
				if ("oauth2".equals(loginMethod)) {
					String returnTo = URLEncoder.encode(auth0LogoutReturnTo, StandardCharsets.UTF_8);
					String auth0LogoutUrl = String.format(
							"https://%s/v2/logout?returnTo=%s&client_id=%s",
							auth0LogoutDomain, returnTo, auth0LogoutClientId
					);
					response.setAuth0LogoutUrl(auth0LogoutUrl);
					log.info("OAuth2 logout for token - Auth0 session termination URL returned");
				} else {
					log.info("Password logout - token blacklisted");
				}
			} catch (Exception e) {
				log.warn("Logout called with unreadable token: {}", e.getMessage());
			}
		}
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<OktaInfoResponse> getOktaInfo(String code, String state, String xCorrelationId) {
		// S-1: the client secret is intentionally NOT included in the response.
		OktaInfoResponse response = new OktaInfoResponse();
		response.setClientId(this.clientId);
		response.setCode(code);
		response.setState(state);
		return ResponseEntity.ok(response);
	}

	// G14: exchange a valid refresh token for a fresh access token. Stateless — verify signature +
	// expiry, blacklist-check, then reissue. Errors surface as domain/JWT exceptions → 400/401.
	// O-5b: the refresh token now arrives in the standard Authorization: Bearer <refreshToken> header.
	@Override
	public ResponseEntity<RefreshResponse> refreshToken(String xCorrelationId) {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			throw new BadRequestException("Missing or malformed Authorization header");
		}
		String rawJwt = header.substring(7);
		if (tokenBlacklistService.isBlacklisted(rawJwt)) {
			throw new UnauthorizedException("Refresh token is no longer valid");
		}
		// Verify signature + not-expired; a refresh token's subject is the uId. Throws → 401 via advice.
		String uId = jwtService.extractSubject(header);
		// Preserve the original login method (oauth2 vs password) so logout can still tear down Auth0.
		String loginMethod = jwtService.extractLoginMethod(header);
		if (loginMethod == null) {
			loginMethod = "password";
		}
		CustomUserDetails userDetails = (CustomUserDetails) userDetailsServiceImpl.loadUserByUserId(uId);

		RefreshResponse response = new RefreshResponse(
				jwtService.genToken(userDetails, loginMethod), userDetails.getuId(), userDetails.getUsername());
		return ResponseEntity.ok(response);
	}
}
