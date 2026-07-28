package io.linkinben.springbootsecurityjwt.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.jwt.KeyProvider;
import io.linkinben.springbootsecurityjwt.jwt.TokenFactory;
import io.linkinben.springbootsecurityjwt.jwt.TokenType;

/**
 * Parses/verifies JWTs and exposes the token-creation API. Key ownership lives in {@link KeyProvider}
 * and token building in {@link TokenFactory} (Factory pattern, F2) — this service keeps the same
 * public signatures so no callers or existing tests change.
 */
@Slf4j
@Service
public class JwtService {

    private final KeyProvider keys;
    private final TokenFactory tokenFactory;

    public JwtService(KeyProvider keys, TokenFactory tokenFactory) {
        this.keys = keys;
        this.tokenFactory = tokenFactory;
    }

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
                return Jwts.parser().verifyWith(keys.getSigningKey()).build()
                        .parseSignedClaims(jwt).getPayload();
            } catch (ExpiredJwtException e) {
                throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Access Token Expired!");
            }
        }

        if (token.startsWith("Authorization ")) {
            String jwt = token.substring(14);
            try {
                return Jwts.parser().verifyWith(keys.getSigningKey()).build()
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
        return tokenFactory.build(TokenType.ACCESS, userDetails.getUsername(), claims);
    }

    public String extractLoginMethod(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.get("loginMethod", String.class) : null;
    }

    public String genRefreshToken(UserDetails userDetails) {
        return genRefreshToken(userDetails, "password");
    }

    // Carry loginMethod in the refresh token so a refreshed access token can preserve it (e.g. an
    // OAuth2 session must stay "oauth2" across refreshes — otherwise logout can't build the Auth0 URL).
    public String genRefreshToken(UserDetails userDetails, String loginMethod) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("loginMethod", loginMethod);
        CustomUserDetails refreshTokenDetail = (CustomUserDetails) userDetails;
        return tokenFactory.build(TokenType.REFRESH, refreshTokenDetail.getuId(), claims);
    }

    public String genCredentialToken(String subject) {
        return tokenFactory.build(TokenType.CREDENTIAL, subject, new HashMap<>());
    }

    public String extractCredentialSubject(String token) {
        return Jwts.parser()
                .verifyWith(keys.getCredentialKey())
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
