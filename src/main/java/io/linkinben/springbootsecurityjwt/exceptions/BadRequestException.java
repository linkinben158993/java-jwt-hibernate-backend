package io.linkinben.springbootsecurityjwt.exceptions;

/** Thrown for invalid/unprocessable input. Mapped to HTTP 400 by GlobalExceptionHandler. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
