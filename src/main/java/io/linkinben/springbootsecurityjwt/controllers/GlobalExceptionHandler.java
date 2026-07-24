package io.linkinben.springbootsecurityjwt.controllers;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.jsonwebtoken.ExpiredJwtException;
import io.linkinben.springbootsecurityjwt.dtos.ErrorResponse;
import io.linkinben.springbootsecurityjwt.exceptions.BadRequestException;
import io.linkinben.springbootsecurityjwt.exceptions.DuplicateResourceException;
import io.linkinben.springbootsecurityjwt.exceptions.ForbiddenOperationException;
import io.linkinben.springbootsecurityjwt.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Central exception → HTTP response mapping. Replaces the per-controller try/catch boilerplate and
 * the previous half-broken CustomErrorController. Produces the consistent {@link ErrorResponse}
 * envelope and never leaks stack traces.
 *
 * NOTE on AuthorizationDeniedException: this global default is 403. UserAPIController keeps a
 * controller-LOCAL @ExceptionHandler that overrides it to 404 for user resources (hide existence);
 * Spring prefers a controller-local handler over @ControllerAdvice, so that override wins there.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "Not found", e.getMessage(), "ERR_NOT_FOUND");
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException e) {
        return build(HttpStatus.CONFLICT, "Conflict", e.getMessage(), "ERR_DUPLICATE");
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
        return build(HttpStatus.BAD_REQUEST, "Bad request", e.getMessage(), "ERR_BAD_REQUEST");
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenOperationException e) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", e.getMessage(), "ERR_FORBIDDEN");
    }

    // Global default for method-security denials → 403 (UserAPIController overrides to 404 locally).
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthzDenied(AuthorizationDeniedException e) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "Access denied", "ERR_FORBIDDEN");
    }

    // Failed login (BadCredentialsException etc.) — preserves the existing 400-on-bad-credentials contract.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        return build(HttpStatus.BAD_REQUEST, "Bad Credential!", "Access Denied!", "ERR_BAD_CREDENTIAL");
    }

    // Reachable from the JWT filter via HandlerExceptionResolver (RequestFilterConfig).
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpired(ExpiredJwtException e) {
        return build(HttpStatus.UNAUTHORIZED, "Bad token", "Access token expired", "ERR_TOKEN_EXPIRED");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", detail, "ERR_VALIDATION");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "Bad request", "Malformed request body", "ERR_MALFORMED_BODY");
    }

    // Manual JSON parsing inside a handler (e.g. a malformed OAuth2 credential) → 400, not 500.
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ErrorResponse> handleJsonProcessing(JsonProcessingException e) {
        return build(HttpStatus.BAD_REQUEST, "Bad request", "Malformed data", "ERR_MALFORMED_BODY");
    }

    // Last resort — log the detail server-side, never leak it to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Server error", "Something went wrong", "ERR_SERVER");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String title, String message, String errCode) {
        return ResponseEntity.status(status).body(new ErrorResponse(title, message, errCode));
    }
}
