package io.linkinben.springbootsecurityjwt.configs;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;

@Slf4j
@Component
public class RequestFilterConfig extends OncePerRequestFilter {

	@Autowired
	private JWTUtils jwtUtils;

	@Autowired
	private UserDetailsServiceImpl userDetailsServiceImpl;

	@Autowired
	private TokenBlacklistService tokenBlacklistService;

	@Autowired
	@Qualifier("handlerExceptionResolver")
	private HandlerExceptionResolver resolver;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		final String authorizationHeader = request.getHeader("access_token");
		final String isRefreshToken = request.getHeader("refresh_token");

		String username = null;
		try {
			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				String rawJwt = authorizationHeader.substring(7);
				if (tokenBlacklistService.isBlacklisted(rawJwt)) {
					// Don't set auth context — protected endpoints return 401 via AuthenticationEntryPoint;
					// permitAll endpoints (e.g. /api/auth/logout itself) still pass through.
					log.debug("Rejected blacklisted token");
					filterChain.doFilter(request, response);
					return;
				}
				username = jwtUtils.extractSubject(authorizationHeader);
				log.debug("Access token subject: {}", username);
			}


			// Check user details
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = this.userDetailsServiceImpl.loadUserByUsername(username);
				log.debug("Authenticating user: {}", userDetails.getUsername());
				if (jwtUtils.validateToken(authorizationHeader, userDetails)) {
					UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					usernamePasswordAuthenticationToken
							.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
				}
			}
			
			filterChain.doFilter(request, response);
			
		} catch (ExpiredJwtException e) {
			log.warn("Access token expired, attempting refresh token fallback");
			String uId = null;
			// Todo: Do something with refresh token if appended in request headers!
			if (authorizationHeader != null && isRefreshToken.startsWith("Authorization ")) {

				try {
					uId = jwtUtils.extractSubject(isRefreshToken);
					log.debug("Refresh token subject: {}", uId);
					authorizeRefreshToken(e, request, uId);
					filterChain.doFilter(request, response);
				} catch (ExpiredJwtException e1) {
					// TODO: Force login in web client if refresh token expires as well!
					resolver.resolveException(request, response, null, e1);
				}
			}
		} catch (JwtException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
		}
	}

	private void authorizeRefreshToken(ExpiredJwtException ex, HttpServletRequest request, String subject) {
		UserDetails userDetails = this.userDetailsServiceImpl.loadUserByUserId(subject);
		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
				userDetails, null, userDetails.getAuthorities());
		usernamePasswordAuthenticationToken
				.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
	}
}
