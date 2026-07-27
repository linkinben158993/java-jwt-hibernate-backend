package io.linkinben.springbootsecurityjwt.exceptions;

/** Thrown when a request's credentials are absent, invalid, or revoked. Mapped to HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
