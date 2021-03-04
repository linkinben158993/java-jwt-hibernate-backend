package io.linkinben.springbootsecurityjwt.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.ExpiredJwtException;

@RestControllerAdvice
public class CustomErrorController {
	@ExceptionHandler(RuntimeException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorDTO processRuntimeException(RuntimeException e) {
		if (e instanceof ExpiredJwtException) {
			return createErrorDTO(HttpStatus.BAD_REQUEST, "Bad Token", "Your token has expired!", e);
		}
		return null;
	}

	private ErrorDTO createErrorDTO(HttpStatus status, String title, String message, Exception e) {
		return new ErrorDTO(title, message);
	}

	private class ErrorDTO {

		private String title;

		private String message;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public ErrorDTO(String title, String message) {
			this.title = title;
			this.message = message;
		}
	}
}
