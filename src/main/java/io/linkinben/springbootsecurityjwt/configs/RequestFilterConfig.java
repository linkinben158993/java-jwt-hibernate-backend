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
		final String isRefreshToken = request.getHeader("refresh_token");

		String username = null;

		try {
			// Extract token with correct header and token is expired?
			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				username = jwtUtils.extractSubject(authorizationHeader);
				System.out.println("Subject: " + username);
			}


			// Check user details
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = this.userDetailsServiceImpl.loadUserByUsername(username);
				System.out.println("User details: " + userDetails.getUsername().toString());
				System.out.println(userDetails.toString());
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
			// If refresh token is in request
			System.out.println("Access Token expired do something!");
			String uId = null;
			// Todo: Do something with refresh token if appended in request headers!
			if (authorizationHeader != null && isRefreshToken.startsWith("Authorization ")) {

				try {
					uId = jwtUtils.extractSubject(isRefreshToken);
					System.out.println("Subject: " + uId);					
					authorizeRefreshToken(e, request, uId);
					filterChain.doFilter(request, response);
				} catch (ExpiredJwtException e1) {
					// TODO: Force login in web client if refresh token expires as well!
					resolver.resolveException(request, response, null, e1);
				}
			}
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
