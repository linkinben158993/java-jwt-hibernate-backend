package io.linkinben.springbootsecurityjwt.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;

@Service
public class JWTUtils {
	private String SECRET_KEY = "AnJWT";
	private String SECRET_CREDENTIAL = "HelloWorld";
	// Expire of 10 hours: 10 * 1000 * 60 * 60 Test 1 minutes: 1000 * 60
	private int EXPIRATION = 10 * 1000 * 60 * 60; // Currently set for 10 hours

	// Expire of 7 * 24 hours: 7 * 24 * 1000 * 60 * 60 Test 1 minutes: 1000 * 60
	private int EXPIRATION_REFRESH = 7 * 24 * 1000 * 60 * 60; // Currently set for 7 days

	// Extract username from jwt token
	public String extractSubject(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	// Extract expiration date from jwt token
	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	// Generic method for extracting token's claim to certain Object
	public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
		final Claims claims = extractAllClaims(token);
		String userId = claims.get("uId", String.class);
		System.out.println(userId);
		return claimResolver.apply(claims);
	}

	// Extract all claims from JWT token this should already check token expired!
	private Claims extractAllClaims(String token) {
		String jwt = null;
		String jwt_refresh = null;

		if (token.startsWith("Bearer ")) {
			jwt = token.substring(7);
			try {
				return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(jwt).getBody();
			} catch (ExpiredJwtException e) {
				throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Access Token Expired!", null);
			}

		}

		if (token.startsWith("Authorization ")) {
			jwt_refresh = token.substring(14);

			try {
				return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(jwt_refresh).getBody();
			} catch (ExpiredJwtException e) {
				throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Refresh Token Expired!", null);
			}
		}

		return null;
	}

	// Check if token expired unnecessary
//	private Boolean tokenIsExpired(String token) {
//		return extractExpiration(token).before(new Date());
//	}

	// Generate token from given userDetails
	// Custom claims can be add when token is initialized using custom user details in DTO class
	public String genToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		String uId = ((CustomUserDetails) userDetails).getuId();
		String uFullName = ((CustomUserDetails) userDetails).getuFullName();
		System.out.println(uId);
		claims.put("uId", uId);
		claims.put("uFullName", uFullName);
		return initToken(claims, userDetails.getUsername());
	}

	// Initialize jwt token
	private String initToken(Map<String, Object> claims, String subject) {
		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))

				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
				.signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
	}

	public String genRefreshToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		CustomUserDetails refreshTokenDetail = (CustomUserDetails) userDetails;
		return initRefreshToken(claims, refreshTokenDetail.getuId());
	}

	// Initialized refresh token
	private String initRefreshToken(Map<String, Object> claims, String subject) {
		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))

				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_REFRESH))
				.signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
	}
	
	public String genCredentialToken(String userDetails) {
		Map<String, Object> claims = new HashMap<>();
		return initCredentialToken(claims, userDetails);
	}
	
	// Initialized credential token 
	private String initCredentialToken(Map<String, Object> claims, String subject) {
		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))

				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_REFRESH))
				.signWith(SignatureAlgorithm.HS256, SECRET_CREDENTIAL).compact();
	}
	
	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractSubject(token);
		// return (username.equals(userDetails.getUsername()) &&
		// !tokenIsExpired(token));
		return username.equals(userDetails.getUsername());
	}
}
