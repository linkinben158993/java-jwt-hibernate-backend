package io.linkinben.springbootsecurityjwt.configs;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;

@Component
public class RequestFilterConfig extends OncePerRequestFilter {

	@Autowired
	private JWTUtils jwtUtils;

	@Autowired
	private UserDetailsServiceImpl userDetailsServiceImpl;

	@Autowired
	@Qualifier("handlerExceptionResolver")
	private HandlerExceptionResolver resolver;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		final String authorizationHeader = request.getHeader("access_token");

		String username = null;
		String jwt = null;

		try {
			// Extract token with correct header and token is expired?
			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				jwt = authorizationHeader.substring(7);
				username = jwtUtils.extractUsername(jwt);
				System.out.println("JWT: " + jwt);
				System.out.println("Username: " + username);
			}

		} catch (ExpiredJwtException e) {
			// If refresh token is in request
			String isRefreshToken = request.getHeader("refresh_token");
			String requestURL = request.getRequestURI();
			System.out.println("Token expired do something!");

			// Todo: Do something with refresh token if appened in request headers!
			resolver.resolveException(request, response, null, e);
		}

		// Check user details
		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = this.userDetailsServiceImpl.loadUserByUsername(username);
			System.out.println("User details: " + userDetails.getUsername().toString());
			System.out.println(userDetails.toString());
			if (jwtUtils.validateToken(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				usernamePasswordAuthenticationToken
						.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			}
		}

		filterChain.doFilter(request, response);
	}
}
