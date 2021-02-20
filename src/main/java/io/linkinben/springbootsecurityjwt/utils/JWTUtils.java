package io.linkinben.springbootsecurityjwt.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
public class JWTUtils {
	private String SECRET_KEY = "AnJWT";

	// Extract username from jwt token
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	// Extract expiration date from jwt token
	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	// Generic method for extracting token's claim to certain Object
	public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
		final Claims claims = extractAllClaims(token);
		return claimResolver.apply(claims);
	}

	// Extract all claims from jwt token
	private Claims extractAllClaims(String token) {
		return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
	}

	// Check if token expired
	private Boolean tokenIsExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	// Generate token from given userDetails
	public String genToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		return initToken(claims, userDetails.getUsername());
	}

	// Initialize jwt token
	private String initToken(Map<String, Object> claims, String subject) {
		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
				// Expire of 10 hours: 1000 * 60 * 60 * 10 Test 1 minutes: 1000 * 60
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60)) // Currently set for one minute
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
	}
	
	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !tokenIsExpired(token));
	}
}
