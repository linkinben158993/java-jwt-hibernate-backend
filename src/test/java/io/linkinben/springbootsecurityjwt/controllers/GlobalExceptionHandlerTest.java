package io.linkinben.springbootsecurityjwt.controllers;

import io.jsonwebtoken.ExpiredJwtException;
import io.linkinben.springbootsecurityjwt.dtos.ErrorResponse;
import io.linkinben.springbootsecurityjwt.exceptions.BadRequestException;
import io.linkinben.springbootsecurityjwt.exceptions.DuplicateResourceException;
import io.linkinben.springbootsecurityjwt.exceptions.ForbiddenOperationException;
import io.linkinben.springbootsecurityjwt.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit test of the exception → response mapping (plan §4.1 progression). */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFound_maps404() {
        ResponseEntity<ErrorResponse> r = handler.handleNotFound(new ResourceNotFoundException("nope"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody().getErrCode()).isEqualTo("ERR_NOT_FOUND");
        assertThat(r.getBody().getMessage()).isEqualTo("nope");
    }

    @Test
    void duplicate_maps409() {
        ResponseEntity<ErrorResponse> r = handler.handleDuplicate(new DuplicateResourceException("dupe"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().getErrCode()).isEqualTo("ERR_DUPLICATE");
    }

    @Test
    void badRequest_maps400() {
        ResponseEntity<ErrorResponse> r = handler.handleBadRequest(new BadRequestException("bad"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void forbiddenOperation_maps403() {
        ResponseEntity<ErrorResponse> r = handler.handleForbidden(new ForbiddenOperationException("no"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void authorizationDenied_maps403() {
        ResponseEntity<ErrorResponse> r = handler.handleAuthzDenied(new AuthorizationDeniedException("denied"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void expiredJwt_maps401() {
        ResponseEntity<ErrorResponse> r = handler.handleExpired(new ExpiredJwtException(null, null, "expired"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unexpected_maps500_andDoesNotLeakDetail() {
        ResponseEntity<ErrorResponse> r = handler.handleUnexpected(new RuntimeException("SECRET internal detail"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody().getMessage()).isEqualTo("Something went wrong");
        assertThat(r.getBody().getMessage()).doesNotContain("SECRET");
    }
}
