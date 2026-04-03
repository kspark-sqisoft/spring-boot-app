package com.noa99kee.board.common;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * 컨트롤러 밖으로 새 나간 예외를 한곳에서 JSON 형태로 바꿉니다.
 *
 * <p>{@code @RestControllerAdvice}는 모든 {@code @RestController}에 공통으로 적용되는 예외 리졸버입니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> handleResponseStatus(
			ResponseStatusException ex, WebRequest request) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
		log.warn(
				"HTTP error response status={} path={} message={}",
				status.value(),
				request.getDescription(false).replace("uri=", ""),
				message);
		return ResponseEntity.status(status)
				.body(
						Map.of(
								"statusCode",
								status.value(),
								"message",
								message,
								"path",
								request.getDescription(false).replace("uri=", ""),
								"timestamp",
								Instant.now().toString()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(
			MethodArgumentNotValidException ex, WebRequest request) {
		String message =
				ex.getBindingResult().getFieldErrors().stream()
						.map(FieldError::getDefaultMessage)
						.collect(Collectors.joining(", "));
		log.warn(
				"Validation failed path={} message={}",
				request.getDescription(false).replace("uri=", ""),
				message.isEmpty() ? ex.getMessage() : message);
		return ResponseEntity.badRequest()
				.body(
						Map.of(
								"statusCode",
								400,
								"message",
								message.isEmpty() ? "Validation failed" : message,
								"path",
								request.getDescription(false).replace("uri=", ""),
								"timestamp",
								Instant.now().toString()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, WebRequest request) {
		log.error("Unhandled error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(
						Map.of(
								"statusCode",
								500,
								"message",
								"Internal server error",
								"path",
								request.getDescription(false).replace("uri=", ""),
								"timestamp",
								Instant.now().toString()));
	}
}
