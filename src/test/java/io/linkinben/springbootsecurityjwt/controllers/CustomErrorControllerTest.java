package io.linkinben.springbootsecurityjwt.controllers;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CustomErrorControllerTest {

    private CustomErrorController controller;

    @BeforeEach
    void setUp() {
        controller = new CustomErrorController();
    }

    // --- 9.1 ExpiredJwtException returns 400 with error body ---
    @Test
    void processRuntimeException_expiredJwt_returns400() {
        ExpiredJwtException ex = new ExpiredJwtException(null, null, "token expired");

        ResponseEntity<Object> result = controller.processRuntimeException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isNotNull();
    }

    // --- 9.2 generic RuntimeException returns null (falls through to default handler) ---
    @Test
    void processRuntimeException_genericException_returnsNull() {
        RuntimeException ex = new RuntimeException("some other error");

        ResponseEntity<Object> result = controller.processRuntimeException(ex);

        assertThat(result).isNull();
    }
}
