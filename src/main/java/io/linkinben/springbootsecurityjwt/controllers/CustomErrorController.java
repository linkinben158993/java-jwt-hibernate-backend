package io.linkinben.springbootsecurityjwt.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.jsonwebtoken.ExpiredJwtException;

@Slf4j
@RestControllerAdvice
public class CustomErrorController extends ResponseEntityExceptionHandler {
	@ExceptionHandler(RuntimeException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<Object> processRuntimeException(RuntimeException e) {
		log.error("RuntimeException caught: {}", e.getMessage(), e);
		if (e instanceof ExpiredJwtException) {
			return buildResponseEntity(createErrorDTO(HttpStatus.BAD_REQUEST, "Bad Token", e.getMessage(), e));
		}
		return null;
	}

	private ResponseEntity<Object> buildResponseEntity(ErrorDTO apiError) {
		return ResponseEntity.status(apiError.getStatus()).body(apiError);
	}

	private ErrorDTO createErrorDTO(HttpStatus status, String title, String message, Exception e) {
		return new ErrorDTO(status, title, message);
	}

	private class ErrorDTO {

		private String title;

		private String message;

		private HttpStatus status;

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

		public HttpStatus getStatus() {
			return status;
		}

		public void setStatus(HttpStatus status) {
			this.status = status;
		}

		public ErrorDTO(HttpStatus status, String title, String message) {
			this.status = status;
			this.title = title;
			this.message = message;
		}
	}
}
