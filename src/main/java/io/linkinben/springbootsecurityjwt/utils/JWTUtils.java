package io.linkinben.springbootsecurityjwt.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;

@Slf4j
@Service
public class JWTUtils {

    // Minimum 32 bytes required for HS256 — replace with a secure secret via env var before deploying
    private final String SECRET_KEY = "AnJWT";
    private final String SECRET_CREDENTIAL = "HelloWorld";

    // Expire of 10 hours
    private final int EXPIRATION = 10 * 1000 * 60 * 60;
    // Expire of 7 days
    private final int EXPIRATION_REFRESH = 7 * 24 * 1000 * 60 * 60;

    private SecretKey toKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) bytes = Arrays.copyOf(bytes, 32);
        return Keys.hmacShaKeyFor(bytes);
    }

    private final SecretKey signingKey = toKey(SECRET_KEY);
    private final SecretKey credentialKey = toKey(SECRET_CREDENTIAL);

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        String userId = claims.get("uId", String.class);
        log.debug("Extracted claim uId: {}", userId);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        if (token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            try {
                return Jwts.parser().verifyWith(signingKey).build()
                        .parseSignedClaims(jwt).getPayload();
            } catch (ExpiredJwtException e) {
                throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Access Token Expired!");
            }
        }

        if (token.startsWith("Authorization ")) {
            String jwt = token.substring(14);
            try {
                return Jwts.parser().verifyWith(signingKey).build()
                        .parseSignedClaims(jwt).getPayload();
            } catch (ExpiredJwtException e) {
                throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Refresh Token Expired!");
            }
        }

        return null;
    }

    public String genToken(UserDetails userDetails) {
        return genToken(userDetails, "password");
    }

    public String genToken(UserDetails userDetails, String loginMethod) {
        Map<String, Object> claims = new HashMap<>();
        String uId = ((CustomUserDetails) userDetails).getuId();
        String uFullName = ((CustomUserDetails) userDetails).getuFullName();
        log.debug("Generating token for uId: {}, loginMethod: {}", uId, loginMethod);
        claims.put("uId", uId);
        claims.put("uFullName", uFullName);
        claims.put("loginMethod", loginMethod);
        return initToken(claims, userDetails.getUsername());
    }

    public String extractLoginMethod(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.get("loginMethod", String.class) : null;
    }

    private String initToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(signingKey)
                .compact();
    }

    public String genRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        CustomUserDetails refreshTokenDetail = (CustomUserDetails) userDetails;
        return initRefreshToken(claims, refreshTokenDetail.getuId());
    }

    private String initRefreshToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_REFRESH))
                .signWith(signingKey)
                .compact();
    }

    public String genCredentialToken(String subject) {
        Map<String, Object> claims = new HashMap<>();
        return initCredentialToken(claims, subject);
    }

    private String initCredentialToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_REFRESH))
                .signWith(credentialKey)
                .compact();
    }

    public String extractCredentialSubject(String token) {
        return Jwts.parser()
                .verifyWith(credentialKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractSubject(token);
        return username.equals(userDetails.getUsername());
    }
}
