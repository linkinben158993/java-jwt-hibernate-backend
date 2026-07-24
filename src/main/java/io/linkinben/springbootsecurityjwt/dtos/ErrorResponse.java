package io.linkinben.springbootsecurityjwt.dtos;

/**
 * Consistent error envelope produced by GlobalExceptionHandler. Mirrors the existing
 * {title, message} success-envelope shape; errCode is optional (null when not applicable).
 * Never carries stack traces or internal detail.
 */
public class ErrorResponse {

    private final String title;
    private final String message;
    private final String errCode;

    public ErrorResponse(String title, String message, String errCode) {
        this.title = title;
        this.message = message;
        this.errCode = errCode;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getErrCode() {
        return errCode;
    }
}
