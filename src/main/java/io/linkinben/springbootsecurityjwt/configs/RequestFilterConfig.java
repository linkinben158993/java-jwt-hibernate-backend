package io.linkinben.springbootsecurityjwt.configs;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.services.JwtService;
import io.linkinben.springbootsecurityjwt.tracing.MdcKeys;

@Slf4j
@Component
public class RequestFilterConfig extends OncePerRequestFilter {

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserDetailsServiceImpl userDetailsServiceImpl;

	@Autowired
	private TokenBlacklistService tokenBlacklistService;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		// Auth endpoints (login, oauth2 login, logout, token refresh) are permitAll and manage their
		// own tokens directly. The access-token filter must not run here — in particular so that
		// POST /api/auth/token/refresh is reachable with an expired or absent access token.
		return request.getRequestURI().startsWith("/api/auth/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		final String authorizationHeader = request.getHeader("access_token");

		String username = null;
		try {
			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				String rawJwt = authorizationHeader.substring(7);
				if (tokenBlacklistService.isBlacklisted(rawJwt)) {
					// Don't set auth context — protected endpoints 401 via AuthenticationEntryPoint.
					log.debug("Rejected blacklisted token");
					filterChain.doFilter(request, response);
					return;
				}
				username = jwtService.extractSubject(authorizationHeader);
				log.debug("Access token subject: {}", username);
			}

			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = this.userDetailsServiceImpl.loadUserByUsername(username);
				if (jwtService.validateToken(authorizationHeader, userDetails)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
					// O6: per-user tracing — enrich MDC with the principal id. Cleared by TracingFilter.
					if (userDetails instanceof CustomUserDetails cud) {
						MDC.put(MdcKeys.USER_ID, cud.getuId());
					}
				}
			}

			filterChain.doFilter(request, response);

		} catch (ExpiredJwtException e) {
			// G14: an expired access token → 401. The client obtains a new one via
			// POST /api/auth/token/refresh (no more in-filter auto-refresh).
			log.warn("Access token expired");
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access token expired");
		} catch (JwtException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
		}
	}
}
