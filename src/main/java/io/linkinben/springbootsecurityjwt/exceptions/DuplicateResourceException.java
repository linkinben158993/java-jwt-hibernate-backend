package io.linkinben.springbootsecurityjwt.exceptions;

/** Thrown when creating a resource that already exists. Mapped to HTTP 409 by GlobalExceptionHandler. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
