package io.linkinben.springbootsecurityjwt.configs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


// Research more
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private AuthenticationHandler customAuthHandler;

	@Autowired
	private UserDetailsServiceImpl myUserDetailService;

	@Autowired
	private RequestFilterConfig requestFilterConfig;

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Test password should be hashed
		return new BCryptPasswordEncoder();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(myUserDetailService).passwordEncoder(passwordEncoder());
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
//		http.cors().configurationSource(corsConfigurationSource());
		http.cors();
		http.csrf().disable().authorizeRequests()
				// Public end-points and apis
				.antMatchers("/home/*").permitAll().antMatchers("/authenticate/*").permitAll().antMatchers("/oauth/*")
				.permitAll().antMatchers("/error/*").permitAll().antMatchers("/api/user/register").permitAll()
				.antMatchers("/ws/**", "/topic", "/app/**").permitAll()
				// Restricted apis
				.antMatchers("/api/user").hasRole("ADMIN")
				// Only admin can add another role
				.antMatchers("/api/role/add").hasRole("ADMIN")
				// Swagger resources and end-points
				.antMatchers("/js/**", "/css/**", "/csrf").permitAll().antMatchers("/swagger-ui.html").permitAll()
				.anyRequest().authenticated().and().sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and().oauth2Login()
				.successHandler(this.customAuthHandler.successHandler).failureHandler(this.customAuthHandler.failureHandler)
				.and().oauth2ResourceServer().jwt();
		http.addFilterBefore(requestFilterConfig, UsernamePasswordAuthenticationFilter.class);
	}

	// Research more
//	@Bean
//	protected CorsConfigurationSource corsConfigurationSource() {
//		CorsConfiguration configuration = new CorsConfiguration();
//		// Credential true must not add allowed origin
//		configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
//		configuration.setAllowedMethods(Arrays.asList("GET", "POST"));
//		configuration.setAllowCredentials(true);
//		configuration.addAllowedOrigin("http://localhost:4200");
//		configuration.addAllowedHeader("*");
//		configuration.addAllowedMethod("*");
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", configuration);
//		return source;
//	}


//	@Bean
//	public ClientRegistrationRepository clientRegistrationRepository() {
//		return new InMemoryClientRegistrationRepository(this.googleClientRegistration());
//	}
//
//	private ClientRegistration googleClientRegistration() {
//		return ClientRegistration.withRegistrationId("google")
//				.clientId("1033780508811-lb1qd87jg9v0r95amq57t7gar4brgq2g.apps.googleusercontent.com")
//				.clientSecret("_i1PKex-0z6JLHoIRVVcrRer").clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
//				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//				.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
//				.scope("openid", "profile", "email", "address", "phone")
//				.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
//				.tokenUri("https://www.googleapis.com/oauth2/v4/token")
//				.userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
//				.userNameAttributeName(IdTokenClaimNames.SUB).jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
//				.clientName("Google").build();
//	}

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		return new InMemoryClientRegistrationRepository(this.oktaClientRegistration());
	}

	private ClientRegistration oktaClientRegistration() {
		return ClientRegistration
				.withRegistrationId("okta")
				.clientId("0oaw601j5L0dfYgMP5d6")
				.clientSecret("4ZILVtQXrhO2pcKN6IQzmSDOxlEFMm9LPQNdrlhH")
				.clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.scope("openid", "profile", "email", "address", "phone")
				.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
				.issuerUri("https://dev-63754972.okta.com/oauth2/default")
				.authorizationUri("https://dev-63754972.okta.com/oauth2/default/v1/authorize")
				.userInfoUri("https://dev-63754972.okta.com/oauth2/default/v1/userinfo")
				.tokenUri("https://dev-63754972.okta.com/oauth2/default/v1/token")
				.userNameAttributeName(IdTokenClaimNames.SUB)
				.jwkSetUri("https://dev-63754972.okta.com/oauth2/default/v1/keys")
				.build();
	}
	
	// Config Whitelist url for swagger
	private static final String[] AUTH_WHITELIST = { "/swagger-resources/**", "/swagger-ui.html", "/v2/api-docs",
			"/webjars/**", };

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers(AUTH_WHITELIST);
	}

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}
}
