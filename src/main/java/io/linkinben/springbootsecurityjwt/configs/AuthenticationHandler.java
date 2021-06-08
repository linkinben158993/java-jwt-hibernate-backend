package io.linkinben.springbootsecurityjwt.configs;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.linkinben.springbootsecurityjwt.utils.JWTUtils;

@Component
public class AuthenticationHandler extends SavedRequestAwareAuthenticationSuccessHandler {
	private ObjectMapper objectMapper = new ObjectMapper();

	public CustomSuccessHandler successHandler = new CustomSuccessHandler();
	public CustomFailureHandler failureHandler = new CustomFailureHandler();

	public class CustomSuccessHandler implements AuthenticationSuccessHandler {

		@Override
		public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
				Authentication authentication) throws IOException, ServletException {
			response.setStatus(HttpStatus.OK.value());
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("uIdGoogle", authentication.getName());

			OAuth2User extractPrincipal = (OAuth2User) authentication.getPrincipal();
			Map<String, Object> info = extractPrincipal.getAttributes();
			data.put("email", info.get("email"));
			data.put("info", info);
			data.put("timestamp", Calendar.getInstance().getTime());

			JWTUtils jwtUtils = new JWTUtils();
//			response.getOutputStream().println(objectMapper.writeValueAsString(data));
			response.sendRedirect("http://localhost:4200/login/"
					+ jwtUtils.genCredentialToken(objectMapper.writeValueAsString(data)));
		}

	}

	public class CustomFailureHandler implements AuthenticationFailureHandler {

		@Override
		public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
				AuthenticationException exception) throws IOException, ServletException {
			System.out.println("Failed");
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("timestamp", Calendar.getInstance().getTime());
			data.put("exception", exception.getMessage());

//			response.getOutputStream().println(objectMapper.writeValueAsString(data));
			response.sendRedirect("http://localhost:4200/login/" + objectMapper.writeValueAsString(data));
		}

	}
}
